package jobby

import cats.data.Kleisli
import cats.effect.*
import cats.implicits.*
import jobby.spec.*
import org.http4s.*
import org.http4s.dsl.io.*
import scribe.Scribe
import smithy4s.http4s.SimpleRestJsonBuilder

import java.nio.file.Paths

import users.*
import health.*

def Routes(
    db: Database,
    config: AppConfig,
    logger: Scribe[IO],
    timeCop: TimeCop
): Resource[IO, HttpApp[IO]] =
  def handleErrors(routes: HttpRoutes[IO]) =
    routes.orNotFound.onError { exc =>
      Kleisli(request => logger.error("Request failed", request.toString, exc))
    }

  val auth = HttpAuth(config.jwt, logger)

  for
    companies <- SimpleRestJsonBuilder
      .routes(CompaniesServiceImpl(db, auth))
      .resource

    jobs <- SimpleRestJsonBuilder
      .routes(JobServiceImpl(db, auth, timeCop))
      .resource

    users <- SimpleRestJsonBuilder
      .routes(UserServiceImpl(db, auth, logger, config.http.deployment))
      .resource

    health <- SimpleRestJsonBuilder.routes(HealthServiceImpl).resource
  yield handleErrors(health <+> jobs <+> companies <+> users <+> Static.routes)
  end for
end Routes

object Static:
  def routes =
    val indexHtml = StaticFile
      .fromResource[IO](
        "index.html",
        None,
        preferGzipped = true
      )
      .getOrElseF(NotFound())

    HttpRoutes.of[IO] {
      case req @ GET -> Root / "assets" / filename
          if filename.endsWith(".js") || filename.endsWith(".js.map") =>
        StaticFile
          .fromResource[IO](
            Paths.get("assets", filename).toString,
            Some(req),
            preferGzipped = true
          )
          .getOrElseF(NotFound())
      case req @ GET -> Root        => indexHtml
      case req if req.method == GET => indexHtml

    }
  end routes
end Static
