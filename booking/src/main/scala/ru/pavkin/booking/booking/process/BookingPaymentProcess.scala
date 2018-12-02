package ru.pavkin.booking.booking.process

import cats.Monad
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import ru.pavkin.booking.booking.booking.Bookings
import ru.pavkin.payment.event.PaymentReceived

class BookingPaymentProcess[F[_]: Monad](bookings: Bookings[F], logger: Logger[F])
    extends (PaymentReceived => F[Unit]) {

  def apply(evt: PaymentReceived): F[Unit] =
    evt.bookingId match {
      case None => ().pure[F] // not booking related payment, ignoring
      case Some(id) =>
        bookings(id).receivePayment(evt.paymentId).flatMap {
          case Right(_) =>
            logger.info(s"Payment received successfully for booking $id of client ${evt.clientId}")
          case Left(rej) =>
            logger
              .warn(
                s"Failed to apply received payment for booking $id of client ${evt.clientId}. Reason: $rej"
              )

        }
    }
}
