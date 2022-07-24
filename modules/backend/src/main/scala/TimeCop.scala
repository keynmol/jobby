package jobby

import java.time.OffsetDateTime
import cats.effect.IO
import java.time.ZoneOffset
import smithy4s.Timestamp
import smithy4s.Newtype

trait TimeCop:
  def nowODT: IO[OffsetDateTime]
  def timestamp: IO[Timestamp] = nowODT.map(Timestamp.fromOffsetDateTime)
  def timestampNT(nt: Newtype[Timestamp]): IO[nt.Type] =
    timestamp.map(nt.apply)

object TimeCop:
  val unsafe: TimeCop = new:
    def nowODT = IO.realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
