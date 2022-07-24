package jobby
package tests

import jobby.spec.*
import cats.effect.IO
import org.http4s.ResponseCookie

trait UsersSuite:
  self: JobbySuite =>

  probeTest("Registration and authentication") { probe =>
    import probe.*
    for
      login    <- gen.str(UserLogin, 5 to 50)
      password <- gen.str(UserPassword, 12 to 128)
      _        <- api.users.register(login, password)
      resp     <- api.users.login(login, password)

      refreshCookie <- IO
        .fromOption(resp.cookie)(
          new Exception("Expected a refresh cookie ")
        )
        .map(_.value)
      accessToken  = resp.access_token.value
      authHeader   = AuthHeader("Bearer " + accessToken)
      refreshToken = refreshCookie.split(";")(0).split("=", 2)(1)

      validAccess  <- auth.access(authHeader)
      validRefresh <- auth.refresh(RefreshToken(refreshToken))
    yield expect(validAccess == validRefresh)
    end for
  }

  probeTest("Using wrong credentials") { probe =>
    import probe.*
    for
      login     <- gen.str(UserLogin, 5 to 50)
      login1    <- gen.str(UserLogin, 5 to 50)
      password  <- gen.str(UserPassword, 12 to 128)
      password1 <- gen.str(UserPassword, 12 to 128)
      _         <- api.users.register(login, password)

      ok              <- api.users.login(login, password).attempt
      wrongLogin      <- api.users.login(login1, password).attempt
      wrongPass       <- api.users.login(login, password1).attempt
      everythingWrong <- api.users.login(login1, password1).attempt
    yield expect.all(
      wrongLogin.isLeft,
      wrongPass.isLeft,
      everythingWrong.isLeft
    )
    end for
  }
end UsersSuite
