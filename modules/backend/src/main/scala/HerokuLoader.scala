package jobby

import scala.util.Try

class HerokuLoader(env: Map[String, String]):
  def loadPgCredentials: Option[PgCredentials] =
    env.get("DATABASE_URL").flatMap { url =>
      Try {

        val parsed = new java.net.URI(url)

        val host     = parsed.getHost()
        val port     = parsed.getPort()
        val userInfo = parsed.getUserInfo()
        val dbName   = parsed.getPath().tail // dropping the first slash

        val userName = userInfo.split(":").apply(0)
        val password = userInfo.split(":").apply(1)

        PgCredentials(
          host = host,
          port = port,
          user = userName,
          password = Some(password),
          database = dbName,
          ssl = true
        )
      }.toOption

    }

end HerokuLoader
