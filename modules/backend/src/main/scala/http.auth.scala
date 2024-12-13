package jobby

import cats.effect.IO
import jobby.spec.*
import scribe.Scribe

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class HttpAuth(config: JwtConfig, logger: Scribe[IO]):

  def accessToken(userId: UserId): (String, FiniteDuration) =
    JWT.create(JWT.Kind.AccessToken, userId, config) ->
      config.expiration(
        JWT.Kind.AccessToken
      )

  def refreshToken(userId: UserId): (String, FiniteDuration) =
    JWT.create(JWT.Kind.RefreshToken, userId, config) ->
      config.expiration(
        JWT.Kind.RefreshToken
      )

  def access[A](header: AuthHeader): IO[UserId] =
    if header.value.startsWith("Bearer ") then
      JWT.validate(
        header.value.drop("Bearer ".length),
        JWT.Kind.AccessToken,
        config
      ) match
        case Failure(ex) =>
          logger.error(ex) *>
            IO.raiseError(UnauthorizedError())
        case Success(t) => IO.pure(t)
    else IO.raiseError(UnauthorizedError())
    end if
  end access

  def refresh[A](token: RefreshToken) =
    JWT.validate(
      token.value,
      JWT.Kind.RefreshToken,
      config
    ) match
      case Failure(ex) =>
        logger.error(ex) *> IO.raiseError(UnauthorizedError())
      case Success(t) => IO.pure(t)
  end refresh
end HttpAuth
