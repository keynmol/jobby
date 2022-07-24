package jobby
package tests

import cats.effect.IO
import cats.syntax.all.*

import scribe.Level
import weaver.*

trait JobbySuite extends IOSuite:
  override type Res = Probe

  def probeTest(name: weaver.TestName)(f: Probe => IO[weaver.Expectations]) =
    test(name) { (probe, log) =>
      val dumpLogs = probe.getLogs.flatMap {
        _.sortBy(_.timeStamp).traverse_ { msg =>

          val msgText = msg.logOutput.plainText

          msg.level match
            case Level.Info  => log.info(msgText)
            case Level.Error => log.error(msgText)
            case Level.Warn  => log.warn(msgText)
            case _           => log.debug(msgText)
        }
      }

      f(probe).attempt
        .flatTap(_ => dumpLogs)
        .flatMap(IO.fromEither)
    }
end JobbySuite
