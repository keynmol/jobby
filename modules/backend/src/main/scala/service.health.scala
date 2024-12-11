package jobby
package health

import cats.effect.IO
import jobby.spec.*

object HealthServiceImpl extends HealthService[IO]:
  override def healthCheck() =
    IO(HealthCheckOutput(service = Some("ok")))
