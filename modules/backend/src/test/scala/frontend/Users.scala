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

case class Resources(
    probe: Probe,
    pw: PlaywrightRuntime
)

class UsersSpec(global: GlobalRead) extends FrontendSuite(global):
  frontendTest("landing page") { (probe, pc, pf) =>
    import pc.*
    for
      _ <- page(_.navigate(probe.serverUri.toString))
      _ <- eventually(page(_.title())) { title =>
        expect.same(title, "Jobby: latest")
      }
      _ <- locator("h1:text=Jobby").map(_.first())
      _ <- locator("h1:text=Latest jobs").map(_.first())
      _ <- locator(""".nav-link:has-text("Register")""").map(_.first())
      _ <- locator(""".nav-link:has-text("Login")""").map(_.first())
    yield success
    end for
  }

  frontendTest("register and login") { (probe, pc, pf) =>
    import pc.*, pf.*

    for
      _ <- page(_.navigate(probe.serverUri.toString))
      _ <- locator(""".nav-link:has-text("Register")""")
        .map(_.first().click())
      login    <- probe.gen.str(UserLogin, 10 to 15)
      password <- probe.gen.str(UserPassword, 12 to 16)
      _        <- submitRegistration(login, password)
      _        <- submitLogin(login, password)

      _ <- locator(""".nav-link:has-text("Add job")""").map(_.first())
      _ <- locator(""".nav-link:has-text("Add a company")""").map(_.first())
      _ <- locator(""".nav-link:has-text("Profile")""").map(_.first())
      _ <- locator(""".nav-link:has-text("Logout")""").map(_.first())
    yield success
    end for
  }

  frontendTest("add company and render its page") { (probe, pc, pf) =>
    import pc.*, probe.*, pf.*
    val frg = Fragments(probe)

    for
      _ <- page(_.navigate(probe.serverUri.toString))
      _ <- locator(""".nav-link:has-text("Register")""")
        .map(_.first().click())
      login    <- probe.gen.str(UserLogin, 10 to 15)
      password <- probe.gen.str(UserPassword, 12 to 16)
      _        <- submitRegistration(login, password)
      _        <- submitLogin(login, password)

      addCompany <- locator(""".nav-link:has-text("Add a company")""")
        .map(_.click())

      attributes <- frg.companyAttributes

      _ <- submitCompany(attributes)

      // ensure we were redirected to the company's page
      _ <- eventually(page(_.url).map(Uri.unsafeFromString)) { u =>
        val path = u.path.dropEndsWithSlash

        expect(path.segments.size == 2) &&
        expect(path.segments.headOption.exists(_.encoded == "company"))
      }

      name <- locator("#company-profile-name").map(_.innerText())
      description <- locator("#company-profile-description")
        .map(_.innerText())
      url <- locator("#company-profile-url").map(_.innerText())
    yield expect
      .all(
        name != attributes.name.value,
        url == attributes.url.value,
        description == attributes.description.value
      )
    end for
  }

end UsersSpec

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