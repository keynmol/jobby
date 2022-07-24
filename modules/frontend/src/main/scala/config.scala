package frontend

import scala.concurrent.duration.*

object Config:
  inline def tokenRefreshPeriod = 15.seconds
