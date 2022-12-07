package jobby
package health

import spec.*
import cats.effect.IO

object HealthServiceImpl extends HealthService[IO]:
  override def healthCheck() =
    IO(HealthCheckOutput(service = Some("ok")))
