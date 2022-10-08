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

case class Resources(
    probe: Probe,
    pw: PlaywrightRuntime
)

class UsersSpec(global: GlobalRead) extends FrontendSuite(global):
  test("landing page") { pb =>
    getPageContext(pb).evalTap(configure).use { pc =>
      for
        _ <- pc.page(_.navigate(pb.probe.serverUri.toString))

        _ <- eventually(IO.println("comparison") *> pc.page(_.title())) {
          title =>
            expect.same(title, "Jobby: latest")
        }
      yield success
    }
  }

  test("landing page 2") { pb =>
    getPageContext(pb).evalTap(configure).use { pc =>
      for
        _ <- pc.page(_.navigate(pb.probe.serverUri.toString))

        _ <- eventually(pc.page(_.title())) { title =>
          expect.same(title, "Jobby: latest")
        }
      yield success
    }
  }
end UsersSpec
