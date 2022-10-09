package jobby
package tests
package integration

import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all.*
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.flywaydb.core.Flyway
import org.http4s.Uri
import org.http4s.blaze.client.*
import org.http4s.client.Client
import org.http4s.ember.client.*
import org.testcontainers.utility.DockerImageName
import pdi.jwt.JwtAlgorithm.HS256
import skunk.util.Typer.Strategy
import org.http4s.ember.server.EmberServerBuilder
import scribe.cats.*
import org.http4s.blaze.server.*
import org.http4s.server.middleware.RequestLogger
import org.http4s.HttpRoutes.apply
import org.http4s.HttpRoutes
import org.http4s.HttpApp
import org.http4s.Status.apply
import org.http4s.server.middleware.ResponseLogger.apply
import org.http4s.server.middleware.ResponseLogger
import org.http4s.Request
import cats.effect.kernel.Ref

object Fixture:
  private def parseJDBC(url: String) = IO(java.net.URI.create(url.substring(5)))

  private def postgresContainer =
    val start = IO(
      PostgreSQLContainer(
        dockerImageNameOverride = DockerImageName("postgres:14"),
        mountPostgresDataToTmpfs = true
      )
    ).flatTap(cont => IO(cont.start()))

    Resource.make(start)(cont => IO(cont.stop()))
  end postgresContainer

  private def migrate(url: String, user: String, password: String) =
    IO(Flyway.configure().dataSource(url, user, password).load()).flatMap { f =>
      IO(f.migrate())
    }

  def skunkConnection(using natchez.Trace[IO]) =
    postgresContainer
      .evalMap(cont => parseJDBC(cont.jdbcUrl).map(cont -> _))
      .evalTap { case (cont, _) =>
        migrate(cont.jdbcUrl, cont.username, cont.password)
      }
      .flatMap { case (cont, jdbcUrl) =>
        val pgConfig = PgCredentials.apply(
          host = jdbcUrl.getHost,
          port = jdbcUrl.getPort,
          user = cont.username,
          password = Some(cont.password),
          database = cont.databaseName,
          ssl = false
        )

        SkunkDatabase
          .load(pgConfig, skunk)
          .map(pgConfig -> _)

      }

  def resource(using natchez.Trace[IO]): Resource[cats.effect.IO, Probe] =
    import scribe.{Logger, Level}
    val silenceOfTheLogs =
      Seq(
        "org.http4s",
        "org.flywaydb.core",
        "org.testcontainers",
        "🐳 [postgres:14]",
        "🐳 [testcontainers/ryuk:0.3.3]"
      )

    silenceOfTheLogs.foreach { log =>
      Logger(log).withMinimumLevel(Level.Error).replace()
    }
    for
      shutdownLatch <- Resource.eval(IO.ref(false))
      res           <- skunkConnection
      pgConfig  = res._1
      db        = res._2
      appConfig = AppConfig(pgConfig, skunk, http, jwt, misc)
      generator <- Resource.eval(Generator.create)
      timeCop   <- Resource.eval(SlowTimeCop.apply)
      logger    <- InMemoryLogger.build
      routes <- JobbyApp(
        appConfig,
        db,
        logger.scribeLogger,
        timeCop
      ).routes
      latchedRoutes = HttpApp[IO] { case req =>
        shutdownLatch.get.flatMap { deadSkunk =>
          if deadSkunk then
            IO.pure(
              org.http4s
                .Response[IO](
                  org.http4s.Status.InternalServerError
                )
                .withEntity("Skunk is dead, stop sending requests!")
            )
          else routes.run(req)
        }
      }
      uri <- BlazeServerBuilder[IO]
        .withHttpApp(latchedRoutes)
        .bindHttp(0, "localhost")
        .resource
        .map(_.baseUri)

      client <- BlazeClientBuilder[IO].resource.onFinalize(
        shutdownLatch.set(true)
      )
      probe <-
        Probe.build(
          client,
          uri,
          appConfig,
          logger
        )
    yield probe
    end for
  end resource

  import scala.concurrent.duration.*

  val jwt = JwtConfig(
    Secret("hello"),
    HS256,
    _ => "jobby:token",
    _ => 5.minutes,
    _ => "jobby:issuer"
  )

  val skunk = SkunkConfig(
    maxSessions = 10,
    strategy = Strategy.SearchPath,
    debug = false
  )

  import com.comcast.ip4s.*

  val http = HttpConfig(host"localhost", port"9914", Deployment.Local)
  val misc = MiscConfig(latestJobs = 20)

end Fixture
