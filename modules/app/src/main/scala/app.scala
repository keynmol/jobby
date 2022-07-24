package jobby
package app

import cats.effect.*
import cats.syntax.all.*

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.exception.FlywayValidateException
import java.io.File
import java.io.FileInputStream

def migrate(postgres: PgCredentials) =
  import postgres.*
  val url =
    s"jdbc:postgresql://$host:$port/$database"

  val flyway =
    IO(Flyway.configure().dataSource(url, user, password.getOrElse("")).load())
      .flatMap { f =>
        val migrate = IO(f.migrate()).void
        val repair  = IO(f.repair()).void

        migrate.handleErrorWith {
          case _: FlywayValidateException =>
            repair.redeemWith[Unit](
              ex => IO.raiseError(ex),
              _ => migrate
            )
          case other => IO.raiseError(other)
        }
      }

  Resource.eval(flyway)
end migrate

object Main extends IOApp:
  import scala.jdk.CollectionConverters.*

  val fileProps =
    for
      f  <- IO(new File("jobby.opts"))
      is <- IO(new FileInputStream(f))
      props = new java.util.Properties()
      loaded <- IO(props.load(is)).guarantee(IO(is.close()))
    yield props.entrySet.asScala.map { e =>
      e.getKey.asInstanceOf[String] -> e.getValue.asInstanceOf[String]
    }.toMap

  def run(args: List[String]) =
    import natchez.Trace.Implicits.noop

    val logger = scribe.cats.io

    val props = fileProps
      .handleErrorWith(ex =>
        logger.error("Failed to load `jobby.opts`", ex).as(Map.empty)
      )
      .flatTap(mp =>
        logger.info(
          s"Loaded from `jobby.opts`: $mp"
        )
      )

    scribe.info(s"Args: $args")

    Resource
      .eval(props.flatMap { p => AppConfig.load(sys.env, args, p) })
      .evalTap(ac => scribe.cats.io.info(ac.toString))
      .flatMap(JobbyApp.bootstrap(_, logger))
      .flatTap(app => migrate(app.config.postgres))
      .flatMap(jobbyApp =>
        jobbyApp.routes.flatMap(Server(jobbyApp.config.http, _))
      )
      .use(_ => IO.never)
  end run
end Main
