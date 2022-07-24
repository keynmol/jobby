package jobby

import jobby.spec.*
import java.util.UUID
import scala.util.Try
import cats.effect.IO
import scala.util.Failure
import scala.util.Success
import pdi.jwt.exceptions.JwtException
import scala.concurrent.duration.FiniteDuration
import scribe.Scribe

object JWT:
  enum Kind:
    case AccessToken, RefreshToken

  import java.time.Instant
  import pdi.jwt.{JwtUpickle, JwtAlgorithm, JwtClaim}

  def create(kind: Kind, userId: UserId, config: JwtConfig) =
    val claim = JwtClaim(
      issuer = Some("jobby"),
      expiration = Some(
        Instant.now
          .plusSeconds(config.expiration(kind).toSeconds)
          .getEpochSecond
      ),
      issuedAt = Some(Instant.now.getEpochSecond),
      audience = Option(Set(config.audience(kind))),
      subject = Option(userId.value.toString)
    )

    JwtUpickle.encode(claim, config.secretKey.plaintext, config.algorithm)
  end create

  def validate(token: String, kind: Kind, config: JwtConfig): Try[UserId] =
    JwtUpickle
      .decode(token, config.secretKey.plaintext, Seq(config.algorithm))
      .flatMap { claim =>
        val aud      = claim.audience.getOrElse(Set.empty)
        val expected = Set(config.audience(kind))
        if aud == expected then Success(claim)
        else
          Failure(
            new Exception(s"Audience: $aud didn't match expected: $expected")
          )
      }
      .flatMap { claim =>
        claim.subject match
          case None    => Failure(new Exception("no subject in JWT"))
          case Some(i) => Try(UUID.fromString(i)).map(UserId.apply)
      }
end JWT
