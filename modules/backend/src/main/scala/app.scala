package jobby

import cats.effect.*
import scribe.Scribe

class JobbyApp(
    val config: AppConfig,
    db: Database,
    logger: Scribe[IO],
    timeCop: TimeCop
)(using natchez.Trace[IO]):
  def routes = Routes(db, config, logger, timeCop)
end JobbyApp

object JobbyApp:
  def bootstrap(config: AppConfig, logger: Scribe[IO])(using
      natchez.Trace[IO]
  ) =
    for db <- SkunkDatabase.load(config.postgres, config.skunk)
    yield JobbyApp(config, db, logger, TimeCop.unsafe)
