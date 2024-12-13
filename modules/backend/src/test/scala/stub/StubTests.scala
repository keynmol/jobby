package jobby
package tests
package stub

import org.typelevel.otel4s.trace.Tracer.Implicits.noop
import weaver.*

abstract class StubSuite(global: GlobalRead) extends JobbySuite:
  override def sharedResource = Fixture.resource
