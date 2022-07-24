package jobby
package tests
package integration

import cats.effect.*
import cats.effect.std.*
import cats.syntax.all.*
import jobby.spec.*
import natchez.Trace.Implicits.noop
import weaver.*

object Resources extends GlobalResource:
  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    baseResources.flatMap(global.putR(_))

  def baseResources: Resource[IO, Probe] = Fixture.resource

  // Provides a fallback to support running individual tests via testOnly
  def sharedResourceOrFallback(read: GlobalRead): Resource[IO, Probe] =
    read.getR[Probe]().flatMap {
      case Some(value) => Resource.eval(IO(value))
      case None        => baseResources
    }
end Resources

abstract class IntegrationSuite(global: GlobalRead) extends JobbySuite:

  import Resources.*
  override def sharedResource = sharedResourceOrFallback(global)
