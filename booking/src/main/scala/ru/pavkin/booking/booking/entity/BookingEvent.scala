package ru.pavkin.booking.booking.entity

import cats.data.NonEmptyList
import ru.pavkin.booking.common.models._

sealed trait BookingEvent extends Product with Serializable

case class BookingPlaced(clientId: ClientId, concertId: ConcertId, seats: NonEmptyList[Seat])
  extends BookingEvent
case class BookingConfirmed(tickets: NonEmptyList[Ticket])
  extends BookingEvent
case class BookingDenied(reason: String) extends BookingEvent
case class BookingCancelled(reason: String) extends BookingEvent
case class BookingExpired() extends BookingEvent
case class BookingPaid(paymentId: PaymentId) extends BookingEvent
case class BookingSettled() extends BookingEvent
