package jobby

import java.time.OffsetDateTime

import cats.effect.*

object SlowTimeCop:
  def apply: IO[TimeCop] = Ref.of[IO, Int](0).map { daysRef =>
    val start = OffsetDateTime.now

    new TimeCop:
      def nowODT = daysRef.getAndUpdate(_ + 1).map { days =>
        start.plusDays(days)
      }
  }
end SlowTimeCop
