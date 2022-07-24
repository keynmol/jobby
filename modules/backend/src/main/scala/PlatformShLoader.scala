package jobby

import scala.util.Try
import java.util.Base64
import scala.util.control.NonFatal

class PlatformShLoader(env: Map[String, String]):
  private val loaded = env
    .get("PLATFORM_RELATIONSHIPS")
    .flatMap { rels =>
      val decoded = new String(Base64.getDecoder.decode(rels))
      Try(ujson.read(decoded))
        .fold(
          { case NonFatal(error) =>
            scribe.error("Failed to parse PLATFORM_RELATIONSHIPS", error)
            Option.empty
          },
          Option.apply(_)
        )
    }

  def loadPgCredentials(relationshipName: String): Option[PgCredentials] =
    loaded.flatMap { json =>
      try
        val db = json.obj(relationshipName).arr(0).obj

        Some(
          PgCredentials(
            host = db("host").str,
            port = db("port").num.toInt,
            user = db("username").str,
            database = db("path").str,
            password = Some(db("password").str),
            ssl = false
          )
        )
      catch
        case exc =>
          scribe.error("Failed to read relationships configuration", exc)
          None
    }
end PlatformShLoader
