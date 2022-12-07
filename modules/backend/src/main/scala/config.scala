package jobby

import scala.util.Try
import java.util.Base64
import scala.util.control.NonFatal
import cats.effect.IO
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import com.comcast.ip4s.Literals.host
import scala.concurrent.duration.FiniteDuration
import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtHmacAlgorithm
import scala.concurrent.duration.*

case class AppConfig(
    postgres: PgCredentials,
    skunk: SkunkConfig,
    http: HttpConfig,
    jwt: JwtConfig,
    misc: MiscConfig
)

object AppConfig:
  def load(
      env: Map[String, String],
      cliArgs: List[String],
      opts: Map[String, String] = Map.empty
  ) =

    val envWithFallback =
      (env.keySet ++ opts.keySet).flatMap { key =>
        val value = env.get(key).orElse(opts.get(key))

        value.map(v => key -> v)
      }.toMap

    val platformSh = PlatformShLoader(envWithFallback)
    val heroku     = HerokuLoader(envWithFallback)
    val flyio      = FlyIOLoader(envWithFallback)

    val postgres = platformSh
      .loadPgCredentials("database")
      .orElse(flyio.loadPgCredentials)
      .orElse(heroku.loadPgCredentials)
      .getOrElse(PgCredentials.from(envWithFallback))

    val skunkConfig = SkunkConfig(
      maxSessions = 16,
      strategy = skunk.Strategy.SearchPath,
      debug = false
    )

    val http = HttpConfig.fromCliArguments(cliArgs, envWithFallback)

    scribe.info(s"Loaded http: $http")

    val jwt = JwtConfig(
      Secret("what"),
      JwtAlgorithm.HS256,
      {
        case JWT.Kind.AccessToken  => "jobby:token:access"
        case JWT.Kind.RefreshToken => "jobby:token:refresh"
      },
      {
        case JWT.Kind.AccessToken  => 15.minutes
        case JWT.Kind.RefreshToken => 14.days
      },
      { case _ => "urn:jobby:auth" }
    )

    val misc = MiscConfig(
      latestJobs = 20
    )

    IO {
      AppConfig(postgres, skunkConfig, http, jwt, misc)
    }
  end load
end AppConfig

case class JwtConfig(
    secretKey: Secret,
    algorithm: JwtHmacAlgorithm,
    audience: JWT.Kind => String,
    expiration: JWT.Kind => FiniteDuration,
    issuer: JWT.Kind => String
)

case class MiscConfig(
    latestJobs: Int = 20
)

enum Deployment:
  case Live, Local

case class HttpConfig(host: Host, port: Port, deployment: Deployment)
object HttpConfig:
  import com.comcast.ip4s.*
  def fromCliArguments(
      args: List[String],
      env: Map[String, String] = Map.empty
  ) =
    HttpConfig(
      port = args.headOption
        .flatMap(Port.fromString)
        .orElse(env.get("PORT").flatMap(Port.fromString))
        .getOrElse(port"9000"),
      host = host"0.0.0.0",
      deployment = env
        .get("LOCAL_DEPLOYMENT")
        .filter(_ == "true")
        .map(_ => Deployment.Local)
        .getOrElse(Deployment.Live)
    )
end HttpConfig

case class SkunkConfig(
    maxSessions: Int,
    strategy: skunk.Strategy,
    debug: Boolean
)

case class PgCredentials(
    host: String,
    port: Int,
    user: String,
    database: String,
    password: Option[String],
    ssl: Boolean
)

object PgCredentials:
  def from(mp: Map[String, String]) =
    PgCredentials(
      host = mp.getOrElse("PG_HOST", "localhost"),
      port = mp.getOrElse("PG_PORT", "5432").toInt,
      user = mp.getOrElse("PG_USER", "postgres"),
      database = mp.getOrElse("PG_DB", "postgres"),
      password = mp.get("PG_PASSWORD"),
      ssl = false
    )
end PgCredentials
