package ru.pavkin.booking.booking.entity

import java.time.Instant

import cats.data.NonEmptyList
import ru.pavkin.booking.common.models._

sealed trait BookingEvent extends Product with Serializable

case class BookingPlaced(clientId: ClientId, concertId: ConcertId, seats: NonEmptyList[Seat])
    extends BookingEvent
case class BookingConfirmed(tickets: NonEmptyList[Ticket], expiresAt: Option[Instant])
    extends BookingEvent
case class BookingDenied(reason: String) extends BookingEvent
case class BookingCancelled(reason: String) extends BookingEvent
case object BookingExpired extends BookingEvent
case class BookingPaid(paymentId: PaymentId) extends BookingEvent
case object BookingSettled extends BookingEvent
