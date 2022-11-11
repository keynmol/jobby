package jobby
package tests
package frontend

import scala.concurrent.duration.*
import com.indoorvivants.weaver.playwright.*
import org.http4s.*
import natchez.Trace.Implicits.noop
import cats.syntax.all.*
import weaver.*
import cats.effect.*
import java.nio.file.Paths
import jobby.spec.*

class PageFragments(
    pc: PageContext,
    probe: Probe,
    policy: PlaywrightRetry
):
  import pc.*

  import Expectations.*
  import Expectations.Helpers.*

  private def eventually[A](ioa: IO[A])(f: A => Expectations) =
    PlaywrightExpectations.eventually(ioa, policy)(f)

  def submitCompany(attributes: CompanyAttributes) =
    for
      // ensure we're on the right page
      _ <- eventually(page(_.url()).map(Uri.unsafeFromString)) { u =>
        expect.same(
          u.path.dropEndsWithSlash.toAbsolute.renderString,
          "/companies/create"
        )
      }
      _ <- locator("#input-company-name").map(_.fill(attributes.name.value))
      _ <- locator("#input-company-description")
        .map(_.fill(attributes.description.value))
      _ <- locator("#input-company-url").map(_.fill(attributes.url.value))
      _ <- locator("#input-company-submit").map(_.click())
    yield ()

  def submitRegistration(login: UserLogin, password: UserPassword): IO[Unit] =
    for
      // ensure we're on the register page
      _ <- eventually(page(_.title())) { title =>
        expect.same(title, "Jobby: register")
      }

      createAccount <- locator("#credentials-submit")
      _ <- expect.same(createAccount.innerText(), "Create account").failFast[IO]

      loginField    <- locator("#credentials-login")
      passwordField <- locator("#credentials-password")

      _ <- IO(loginField.fill(login.value))
      _ <- IO(passwordField.fill(password.value))

      _ <- IO(createAccount.click())
    yield ()

  def submitLogin(login: UserLogin, password: UserPassword): IO[Unit] =
    for
      // ensure we're on the register page
      _ <- eventually(page(_.title())) { title =>
        expect.same(title, "Jobby: login")
      }

      loginButton <- locator("#credentials-submit")
      _           <- expect.same(loginButton.innerText(), "Login").failFast[IO]

      loginField    <- locator("#credentials-login")
      passwordField <- locator("#credentials-password")

      _ <- IO(loginField.fill(login.value))
      _ <- IO(passwordField.fill(password.value))

      _ <- IO(loginButton.click())
    yield ()
end PageFragments
