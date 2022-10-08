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

class UsersSpec(global: GlobalRead) extends FrontendSuite(global){ 
  test("landing page") { pb =>
    getPageContext(pb).evalTap(configure).use { pc =>
      for
        _ <- pc.page(_.navigate(pb.probe.serverUri.toString))
        _ <- eventually(pc.page(_.title())) { title =>
          expect.same(title, "Jobby: latest")
        }
        _ <- pc.locator("h1:text=Jobby").map(_.first())
        _ <- pc.locator("h1:text=Latest jobs").map(_.first())
        _ <- pc.locator("text=Login").map(_.first())
        _ <- pc.locator("text=Register").map(_.first())
      yield success
    }
  }

  test("register and login") { res =>
    getPageContext(res).evalTap(configure).use { pc =>
      import pc.*
      val pf = PageFragments(pc, res.probe)
      import pf.*

      for
        _        <- page(_.navigate(res.probe.serverUri.toString))
        _        <- locator("text=Register").map(_.first().click())
        login    <- res.probe.gen.str(UserLogin, 10 to 15)
        password <- res.probe.gen.str(UserPassword, 12 to 16)
        _        <- submitRegistration(login, password)
        _        <- submitLogin(login, password)
      yield success
      end for
    }
  }

  case class PageFragments(pc: PageContext, probe: Probe):
    import pc.*

    def submitRegistration(login: UserLogin, password: UserPassword) =
      for
        // ensure we're on the register page
        _ <- eventually(page(_.title())) { title =>
          expect.same(title, "Jobby: register")
        }

        createAccount <- locator("#credentials-submit")
        _ <- expect.same(createAccount.innerText(), "Create account").failFast

        loginField    <- locator("#credentials-login")
        passwordField <- locator("#credentials-password")

        _ <- IO(loginField.fill(login.value))
        _ <- IO(passwordField.fill(password.value))

        _ <- IO(createAccount.click())
      yield ()

    def submitLogin(login: UserLogin, password: UserPassword) =
      for
        // ensure we're on the register page
        _ <- eventually(page(_.title())) { title =>
          expect.same(title, "Jobby: login")
        }

        loginButton <- locator("#credentials-submit")
        _           <- expect.same(loginButton.innerText(), "Login").failFast

        loginField    <- locator("#credentials-login")
        passwordField <- locator("#credentials-password")

        _ <- IO(loginField.fill(login.value))
        _ <- IO(passwordField.fill(password.value))

        _ <- IO(loginButton.click())
      yield ()
  end PageFragments
}
