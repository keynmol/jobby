package jobby

import java.time.OffsetDateTime
import java.time.ZoneOffset

import cats.effect.IO
import smithy4s.Newtype
import smithy4s.Timestamp

trait TimeCop:
  def nowODT: IO[OffsetDateTime]
  def timestamp: IO[Timestamp] = nowODT.map(Timestamp.fromOffsetDateTime)
  def timestampNT(nt: Newtype[Timestamp]): IO[nt.Type] =
    timestamp.map(nt.apply)

object TimeCop:
  val unsafe: TimeCop = new:
    def nowODT = IO.realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
