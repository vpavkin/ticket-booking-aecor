package ru.pavkin.booking.booking.service

import cats.data.NonEmptyList
import ru.pavkin.booking.booking.service.BookingConfirmationService.{ConfirmationFailure, ReleaseFailure}
import ru.pavkin.booking.common.models.{BookingKey, ConcertId, Seat, Ticket}

trait BookingConfirmationService[F[_]] {

  def book(bookingId: BookingKey,
           concertId: ConcertId,
           seats: NonEmptyList[Seat]): F[Either[ConfirmationFailure, NonEmptyList[Ticket]]]

  def release(bookingId: BookingKey): F[Either[ReleaseFailure, Unit]]
}

object BookingConfirmationService {

  sealed trait ConfirmationFailure
  case object SeatsAlreadyBooked extends ConfirmationFailure
  case object UnknownSeats extends ConfirmationFailure
  case object DuplicateSeats extends ConfirmationFailure

  sealed trait ReleaseFailure
  case object UnknownBooking extends ReleaseFailure
}
