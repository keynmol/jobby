package jobby

import org.http4s.ember.server.EmberServerBuilder
import cats.effect.IO
import org.http4s.HttpApp

def Server(config: HttpConfig, app: HttpApp[IO]) =
  EmberServerBuilder
    .default[IO]
    .withPort(config.port)
    .withHost(config.host)
    .withHttpApp(app)
    .build
end Server
