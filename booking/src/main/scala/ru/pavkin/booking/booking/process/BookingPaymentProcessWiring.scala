package ru.pavkin.booking.booking.process

import aecor.data.{ Committable, ConsumerId }
import aecor.distributedprocessing.DistributedProcessing
import cats.effect.{ ConcurrentEffect, Timer }
import ru.pavkin.booking.common.streaming.Fs2Process
import ru.pavkin.payment.event.PaymentReceived
import cats.syntax.all._

class BookingPaymentProcessWiring[F[_]: ConcurrentEffect: Timer](
  source: ConsumerId => fs2.Stream[F, Committable[F, PaymentReceived]],
  process: PaymentReceived => F[Unit]
) {

  val consumerId = ConsumerId("BookingPaymentProcess")

  val processStream: fs2.Stream[F, Unit] =
    source(consumerId).evalMap(c => process(c.value) >> c.commit)

  // Topic has 4 partitions, so we can run up to 4 processes in our cluster
  def processes: List[DistributedProcessing.Process[F]] =
    List.fill(4)(Fs2Process(processStream))

}
