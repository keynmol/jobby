package jobby
package tests
package frontend

import scala.concurrent.duration.*
import com.indoorvivants.weaver.playwright.*
import org.http4s.*
import natchez.Trace.Implicits.noop
import cats.syntax.all.*
import cats.effect.*
import weaver.*
import com.indoorvivants.weaver.playwright.BrowserConfig.Chromium
import com.microsoft.playwright.BrowserType.LaunchOptions
import java.nio.file.Paths

abstract class FrontendSuite(global: GlobalRead)
    extends weaver.IOSuite
    with PlaywrightIntegration:
  override type Res = Resources

  override def sharedResource =
    integration.Fixture.resource.flatMap { pb =>
      PlaywrightRuntime
        .single(browser =
          Chromium(
            Some(
              LaunchOptions()
                .setHeadless(sys.env.contains("CI"))
                .setSlowMo(sys.env.get("CI").map(_ => 0).getOrElse(1000))
            )
          )
        )
        .map { pw =>
          Resources(pb, pw)
        }
    }

  val (poolSize, timeout) =
    if sys.env.contains("CI") then 1 -> 30.seconds
    else 4                           -> 5.seconds

  override def getPlaywright(res: Res): PlaywrightRuntime = res.pw

  override def retryPolicy: PlaywrightRetry =
    PlaywrightRetry.linear(10, 500.millis) // 5 seconds max

  def configure(pc: PageContext) =
    pc.page(_.setDefaultTimeout(timeout.toMillis))

  def frontendTest(
      name: TestName
  )(f: (Probe, PageContext, PageFragments) => IO[Expectations]) =
    test(name) { (res, logs) =>
      getPageContext(res).evalTap(configure).use { pc =>
        def screenshot(pc: PageContext, name: String) =
          val path = Paths.get("playwright-screenshots", name + ".png")
          pc.screenshot(path) *> logs.info(
            s"Screenshot of last known page state is saved at ${path.toAbsolutePath()}"
          )

        def testName = name.name.collect {
          case c if c.isWhitespace => '_'; case o => o
        }

        f(res.probe, pc, PageFragments(pc, res.probe, retryPolicy))
          .guaranteeCase {
            case Outcome.Errored(e) =>
              screenshot(pc, s"error-$testName")
            case Outcome.Succeeded(ioa) =>
              ioa.flatMap { exp =>
                if exp.run.isValid then IO.unit
                else screenshot(pc, s"failure-$testName")
              }
            case _ => IO.unit
          }
      }
    }
  end frontendTest

end FrontendSuite
