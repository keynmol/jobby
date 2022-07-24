package jobby
package tests

import cats.effect.*
import cats.effect.std.*

import scribe.*
import scribe.cats.*

class InMemoryLogger private (
    val logs: IO[Vector[LogRecord]],
    val scribeLogger: Scribe[IO]
)

object InMemoryLogger:
  def build: Resource[IO, InMemoryLogger] =
    Dispatcher[IO].evalMap { disp =>
      Ref.ofEffect(IO(Vector.empty[LogRecord])).map { ref =>
        val handler = scribe.handler.LogHandler(Level.Info) { msg =>
          disp.unsafeRunSync(ref.update(_.appended(msg)))
        }

        val logger =
          scribe.Logger.empty
            .orphan()
            .clearHandlers()
            .withHandler(handler)
            .f[IO]

        new InMemoryLogger(
          ref.get,
          logger
        )
      }
    }
end InMemoryLogger
