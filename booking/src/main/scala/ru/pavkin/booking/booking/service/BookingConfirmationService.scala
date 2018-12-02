package ru.pavkin.booking.booking.service

import java.time.Instant

import cats.data.NonEmptyList
import ru.pavkin.booking.booking.service.BookingConfirmationService.{Confirmation, ConfirmationFailure, ReleaseFailure}
import ru.pavkin.booking.common.models.{BookingKey, ConcertId, Seat, Ticket}

trait BookingConfirmationService[F[_]] {

  def book(bookingId: BookingKey,
           concertId: ConcertId,
           seats: NonEmptyList[Seat]): F[Either[ConfirmationFailure, Confirmation]]

  def release(bookingId: BookingKey): F[Either[ReleaseFailure, Unit]]
}

object BookingConfirmationService {

  case class Confirmation(tickets: NonEmptyList[Ticket], expiresAt: Option[Instant])

  sealed trait ConfirmationFailure
  case object SeatsAlreadyBooked extends ConfirmationFailure
  case object UnknownSeats extends ConfirmationFailure
  case object DuplicateSeats extends ConfirmationFailure

  sealed trait ReleaseFailure
  case object UnknownBooking extends ReleaseFailure
}
