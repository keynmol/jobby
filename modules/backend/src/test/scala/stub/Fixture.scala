package jobby
package tests
package stub

import scala.concurrent.duration.*

import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all.*

import com.comcast.ip4s.*
import org.http4s.Uri
import org.http4s.client.Client
import pdi.jwt.JwtAlgorithm.HS256
import scribe.cats.*
import skunk.util.Typer.Strategy

object Fixture:

  def resource(using natchez.Trace[IO]): Resource[cats.effect.IO, Probe] =
    for
      db      <- Resource.eval(InMemoryDB.create)
      timeCop <- Resource.eval(SlowTimeCop.apply)
      logger  <- InMemoryLogger.build
      routes <- JobbyApp(
        appConfig,
        db,
        logger.scribeLogger,
        timeCop
      ).routes
      client = Client.fromHttpApp(routes)
      generator <- Resource.eval(Generator.create)
      probe <-
        Probe.build(
          client,
          Uri.unsafeFromString("http://localhost"),
          appConfig,
          logger
        )
    yield probe
    end for
  end resource

  val jwt = JwtConfig(
    Secret("hello"),
    HS256,
    _ => "jobby:token",
    _ => 5.minutes,
    _ => "jobby:issuer"
  )
  val skunk = SkunkConfig(
    maxSessions = 0,
    strategy = Strategy.BuiltinsOnly,
    debug = false
  )
  val http = HttpConfig(host"localhost", port"9914", Deployment.Local)
  val misc = MiscConfig(latestJobs = 20)
  val pg   = PgCredentials.from(Map.empty)

  val appConfig =
    AppConfig(pg, skunk, http, jwt, misc)

end Fixture
