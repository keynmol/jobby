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
import org.tpolecat.poolparty.PooledResourceBuilder.apply
import org.tpolecat.poolparty.PooledResourceBuilder

abstract class FrontendSuite(global: GlobalRead)
    extends weaver.IOSuite
    with PlaywrightIntegration:
  override type Res = Resources

  override def sharedResource =
    integration.Fixture.resource.flatMap { pb =>
      PlaywrightRuntime.single().map { pw =>
        Resources(pb, pw)
      }
    }
    // integration.Resources
    //   .sharedResourceOrFallback(global)
    //   .flatMap { probe =>
    //     // PooledResourceBuilder
    //     //   .of(PlaywrightRuntime.single(), 4)
    //     //   .withReporter(ev => IO.println(ev))
    //     //   .build
    //     //   .map(new PooledPlaywrightRuntime(_))
    //     //   .map { pw => Resources(probe, pw) }
    //     //   .onFinalize(IO.println("Closing playwright..."))

    //     PlaywrightRuntime
    //       .single()
    //       .map { pw =>
    //         Resources(probe, pw)
    //       }
    //       .onFinalize(IO.println("Closing playwright..."))
    //   }

  val (poolSize, timeout) =
    if sys.env.contains("CI") then 1 -> 30.seconds
    else 4                           -> 5.seconds

  override def getPlaywright(res: Res): PlaywrightRuntime = res.pw

  override def retryPolicy: PlaywrightRetry =
    PlaywrightRetry.linear(10, 500.millis) // 5 seconds max

  def configure(pc: PageContext) =
    pc.page(_.setDefaultTimeout(timeout.toMillis))
end FrontendSuite

class PooledPlaywrightRuntime(
    pool: Resource[IO, PlaywrightRuntime]
) extends PlaywrightRuntime:
  override def pageContext: Resource[IO, PageContext] =
    pool.flatMap(_.pageContext)
