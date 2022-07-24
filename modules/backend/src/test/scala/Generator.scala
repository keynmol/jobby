package jobby
package tests

import java.util.UUID

import cats.effect.*
import cats.effect.std.*
import cats.syntax.all.*

import smithy4s.Newtype

case class Generator private (random: Random[IO], uuid: UUIDGen[IO]):
  def id(nt: Newtype[UUID]): IO[nt.Type] =
    uuid.randomUUID.map(nt.apply)

  def int(nt: Newtype[Int], min: Int, max: Int): IO[nt.Type] =
    random.betweenInt(min, max).map(nt.apply)

  def url(nt: Newtype[String]): IO[nt.Type] =
    str(nt, 0 to 100).map { v =>
      nt.apply(s"https://${v.value}.com")
    }

  def str(
      toNewType: Newtype[String],
      lengthRange: Range = 0 to 100
  ): IO[toNewType.Type] =
    for
      length <- random.betweenInt(lengthRange.start, lengthRange.end)
      chars  <- random.nextAlphaNumeric.replicateA(length).map(_.mkString)
      str = toNewType.getClass.getSimpleName.toString + "-" + chars
    yield toNewType(str.take(lengthRange.end))

end Generator

object Generator:
  def create: IO[Generator] =
    (Random.scalaUtilRandom[IO], IO(UUIDGen[IO])).mapN(Generator.apply)
