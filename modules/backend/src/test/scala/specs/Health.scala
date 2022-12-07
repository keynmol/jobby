package jobby
package tests

import jobby.spec.*
import cats.effect.IO
import org.http4s.ResponseCookie

trait HealthSuite:
  self: JobbySuite =>

  probeTest("Health check") { probe =>
    import probe.*

    api.health.healthCheck().map { result =>
      expect(result.service == Some("ok"))
    }
  }
end HealthSuite
