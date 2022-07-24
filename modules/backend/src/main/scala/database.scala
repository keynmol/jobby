package jobby

import cats.effect.*

import database.operations.*

trait Database:
  def stream[I, O](query: SqlQuery[I, O]): fs2.Stream[IO, O]

  def vector[I, O](query: SqlQuery[I, O]): IO[Vector[O]] =
    stream(query).compile.toVector

  def option[I, O](query: SqlQuery[I, O]): IO[Option[O]] =
    vector(query).map(_.headOption)
