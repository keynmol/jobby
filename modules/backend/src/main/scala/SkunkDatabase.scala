package jobby

import cats.effect.*
import skunk.*
import cats.implicits.*
import skunk.*
import skunk.implicits.*
import skunk.codec.all.*
import jobby.spec.*
import smithy4s.Newtype
import database.codecs.*
import database.operations.*

object SkunkDatabase:
  def load(postgres: PgCredentials, skunkConfig: SkunkConfig)(using
      natchez.Trace[IO]
  ): Resource[IO, Database] =
    Session
      .pooled[IO](
        host = postgres.host,
        port = postgres.port,
        user = postgres.user,
        database = postgres.database,
        password = postgres.password,
        strategy = skunkConfig.strategy,
        max = skunkConfig.maxSessions,
        debug = skunkConfig.debug,
        ssl = if postgres.ssl then skunk.SSL.Trusted else skunk.SSL.None
      )
      .map(SkunkDatabase(_))
  end load
end SkunkDatabase

class SkunkDatabase(sess: Resource[IO, Session[IO]]) extends Database:
  // we can provide a more efficient version
  override def option[I, O](query: SqlQuery[I, O]): IO[Option[O]] =
    sess.use { s =>
      query.prepare(s).use(_.option(query.input))
    }

  def stream[I, O](query: SqlQuery[I, O]): fs2.Stream[IO, O] =
    for
      sess     <- fs2.Stream.resource(sess)
      prepared <- fs2.Stream.resource(query.prepare(sess))
      q        <- prepared.stream(query.input, 128)
    yield q

  override def vector[I, O](query: SqlQuery[I, O]): IO[Vector[O]] =
    stream(query).compile.toVector
end SkunkDatabase
