package jobby
package tests

import cats.effect.*

import org.http4s.Uri
import org.http4s.client.Client
import scribe.cats.*
import scribe.handler.FunctionalLogHandler
import cats.effect.std.Dispatcher
import scribe.LogRecord
import scribe.handler.LogHandler
import scribe.Level

case class Probe(
    api: Api,
    auth: HttpAuth,
    serverUri: Uri,
    gen: Generator,
    config: AppConfig,
    getLogs: IO[Vector[LogRecord]]
):
  def fragments = Fragments(this)
end Probe

object Probe:
  def build(
      client: Client[IO],
      uri: Uri,
      config: AppConfig,
      logger: InMemoryLogger
  ) =
    Resource.eval {
      for
        gen <- Generator.create
        api <- Api.build(client, uri)
        auth = HttpAuth(
          config.jwt,
          logger.scribeLogger
        )
      yield Probe(api, auth, uri, gen, config, logger.logs)
    }
end Probe
