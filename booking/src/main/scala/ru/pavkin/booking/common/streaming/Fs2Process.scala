package ru.pavkin.booking.common.streaming

import aecor.distributedprocessing.DistributedProcessing._
import cats.effect.ConcurrentEffect
import cats.implicits._
import fs2._
import fs2.concurrent.SignallingRef

object Fs2Process {
  def apply[F[_]](stream: Stream[F, Unit])(implicit F: ConcurrentEffect[F]): Process[F] =
    Process(for {
      signal <- SignallingRef(false)
      running <- F.start(
                  stream
                    .interruptWhen(signal)
                    .compile
                    .drain
                )
    } yield RunningProcess(running.join, signal.set(true)))
}
