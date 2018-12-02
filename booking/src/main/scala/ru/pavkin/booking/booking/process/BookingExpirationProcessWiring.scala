package ru.pavkin.booking.booking.process

import java.time.Instant
import java.util.concurrent.TimeUnit

import aecor.distributedprocessing.DistributedProcessing
import cats.effect.{Clock, ConcurrentEffect, Timer}
import cats.implicits._
import ru.pavkin.booking.common.streaming.Fs2Process

import scala.concurrent.duration.FiniteDuration

class BookingExpirationProcessWiring[F[_]: ConcurrentEffect: Timer](clock: Clock[F],
                                                                    frequency: FiniteDuration,
                                                                    process: Instant => F[Unit]) {

  val processStream: fs2.Stream[F, Unit] =
    fs2.Stream
      .fixedDelay[F](frequency)
      .evalMap(_ => clock.realTime(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli))
      .evalMap(process)

  def processes: List[DistributedProcessing.Process[F]] =
    List(Fs2Process(processStream))

}
