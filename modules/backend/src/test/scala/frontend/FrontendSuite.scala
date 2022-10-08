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
import org.tpolecat.poolparty.PooledResourceBuilder
import com.indoorvivants.weaver.playwright.BrowserConfig.Chromium
import com.microsoft.playwright.BrowserType.LaunchOptions

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
end FrontendSuite
