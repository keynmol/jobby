package jobby 
package tests 
package frontend

import scala.concurrent.duration.*
import com.indoorvivants.weaver.playwright.*
import org.http4s.*
import natchez.Trace.Implicits.noop
import cats.syntax.all.*
import weaver.*

abstract class FrontendSuite(global: GlobalRead)
    extends weaver.IOSuite
    with PlaywrightIntegration:
  override type Res = Resources

  override def sharedResource =
    integration.Resources
      .sharedResourceOrFallback(global)
      .parProduct(PlaywrightRuntime.create(poolSize = poolSize))
      .map(Resources.apply)

  val (poolSize, timeout) =
    if sys.env.contains("CI") then 1 -> 30.seconds
    else 4                           -> 5.seconds

  override def getPlaywright(res: Res): PlaywrightRuntime = res.pw

  override def retryPolicy: PlaywrightRetry =
    PlaywrightRetry.linear(10, 500.millis) // 5 seconds max

  def configure(pc: PageContext) =
    pc.page(_.setDefaultTimeout(timeout.toMillis))
end FrontendSuite
