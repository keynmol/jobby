package jobby
package users

import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

import cats.effect.*
import cats.effect.std.Random
import cats.implicits.*

import jobby.database.operations as op
import jobby.spec.*

import org.http4s.HttpDate
import org.http4s.RequestCookie
import org.http4s.ResponseCookie
import org.http4s.SameSite
import scribe.Scribe

import validation.*

class UserServiceImpl(
    db: Database,
    auth: HttpAuth,
    logger: Scribe[IO],
    deployment: Deployment
) extends UserService[IO]:

  override def login(login: UserLogin, password: UserPassword): IO[Tokens] =
    db.option(op.GetCredentials(login)).flatMap {
      case None => IO.raiseError(CredentialsError("User not found"))
      case Some(id -> hashed) =>
        val seed :: hash :: Nil = hashed.process(_.split(":").toList)
        val requestHash         = Crypto.sha256(seed + ":" + password.value)

        if !requestHash.equalsIgnoreCase(hash) then
          IO.raiseError(CredentialsError("Wrong credentials"))
        else
          val (refresh, maxAgeRefresh) = auth.refreshToken(id)
          val (access, maxAgeAccess)   = auth.accessToken(id)

          Clock[IO].realTime.map { curDate =>
            val instant =
              Instant.ofEpochMilli((curDate + maxAgeRefresh).toMillis)

            Tokens(
              access_token = AccessToken(access),
              cookie = Option(
                Cookie(
                  secureCookie(
                    "refresh_token",
                    refresh,
                    HttpDate.unsafeFromInstant(instant)
                  )
                )
              ),
              expires_in = Some(TokenExpiration(maxAgeAccess.toSeconds.toInt))
            )
          }
        end if
    }

  override def register(login: UserLogin, password: UserPassword): IO[Unit] =
    val validation = (validateUserLogin(login), validateUserPassword(password))
      .traverse(IO.fromEither)

    validation *>
      Crypto
        .hashPassword(password)
        .flatMap { hash =>
          db.option(op.CreateUser(login, hash))
            .onError(ex => logger.error("Registration failed", ex))
            .adaptErr { case _ =>
              ValidationError("Failed to register")
            }
        }
        .void
  end register

  override def refresh(
      refreshToken: Option[RefreshToken],
      logout: Option[Boolean]
  ): IO[RefreshOutput] =
    val extractCookie =
      refreshToken match
        case None => IO.pure(None)
        case Some(cookieString) =>
          IO.fromEither(org.http4s.headers.Cookie.parse(cookieString.value))
            .map(_.values.find(_.name == "refresh_token"))
            .handleError(_ => None)

    if logout.contains(true) then
      IO(
        RefreshOutput(
          access_token = None,
          logout = Some(
            Cookie(
              ResponseCookie(
                "refresh_token",
                "",
                maxAge = Some(0L),
                secure = deployment == Deployment.Live,
                path = Some("/api/users/refresh")
              ).renderString
            )
          ),
          expires_in = TokenExpiration(0)
        )
      )
    else
      extractCookie.flatMap {
        case None =>
          IO.raiseError(
            UnauthorizedError(message = Some("Refresh cookie is missing"))
          )
        case Some(tok) =>
          auth.refresh(RefreshToken(tok.content)).flatMap { userId =>
            val (auth_token, expiresIn) = auth.accessToken(userId)

            IO.pure(
              RefreshOutput(
                access_token = Some(AccessToken(auth_token)),
                expires_in = TokenExpiration(expiresIn.toSeconds.toInt)
              )
            )
          }
      }
    end if
  end refresh

  private def secureCookie(name: String, value: String, expires: HttpDate) =
    ResponseCookie(
      name,
      value,
      httpOnly = true,
      secure = deployment == Deployment.Live,
      path = Some("/api/users/refresh"),
      expires = Some(expires),
      sameSite = Some(SameSite.Strict)
    ).renderString
end UserServiceImpl
