package jobby
package tests
package stub

import weaver.*

import org.typelevel.otel4s.trace.Tracer.Implicits.noop

abstract class StubSuite(global: GlobalRead) extends JobbySuite:
  override def sharedResource = Fixture.resource
