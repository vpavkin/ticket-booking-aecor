package ru.pavkin.booking.booking.entity

import java.time.Instant

import aecor.data.Folded
import cats.data.NonEmptyList
import ru.pavkin.booking.common.models._
import aecor.data.Folded.syntax._

case class BookingState(clientId: ClientId,
                        concertId: ConcertId,
                        seats: NonEmptyList[Seat],
                        tickets: Option[NonEmptyList[Ticket]],
                        status: BookingStatus,
                        expiresAt: Option[Instant],
                        paymentId: Option[PaymentId]) {

  def handleEvent(e: BookingEvent): Folded[BookingState] = e match {
    case _: BookingPlaced => impossible
    case e: BookingConfirmed =>
      copy(tickets = Some(e.tickets), expiresAt = e.expiresAt, status = BookingStatus.Confirmed).next
    case _: BookingDenied | _: BookingCancelled => copy(status = BookingStatus.Canceled).next
    case BookingExpired                         => copy(status = BookingStatus.Canceled).next
    case e: BookingPaid                         => copy(paymentId = Some(e.paymentId)).next
    case BookingSettled                         => copy(status = BookingStatus.Settled).next
  }

}

object BookingState {

  def init(e: BookingEvent): Folded[BookingState] = e match {
    case e: BookingPlaced =>
      BookingState(
        e.clientId,
        e.concertId,
        e.seats,
        None,
        BookingStatus.AwaitingConfirmation,
        None,
        None
      ).next
    case _ => impossible
  }
}
