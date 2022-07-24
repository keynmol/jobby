package jobby
package tests
package stub

import weaver.*

import natchez.Trace.Implicits.noop

abstract class StubSuite(global: GlobalRead) extends JobbySuite:
  override def sharedResource = Fixture.resource
