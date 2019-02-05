package ru.pavkin.booking.booking.service

import java.time.Instant

import cats.data.NonEmptyList
import ru.pavkin.booking.booking.service.TicketReservationService.{
  Reservation,
  ReservationFailure,
  ReleaseFailure
}
import ru.pavkin.booking.common.models.{ BookingKey, ConcertId, Seat, Ticket }

trait TicketReservationService[F[_]] {

  def reserve(bookingId: BookingKey,
              concertId: ConcertId,
              seats: NonEmptyList[Seat]): F[Either[ReservationFailure, Reservation]]

  def release(bookingId: BookingKey): F[Either[ReleaseFailure, Unit]]
}

object TicketReservationService {

  case class Reservation(tickets: NonEmptyList[Ticket], expiresAt: Option[Instant])

  sealed trait ReservationFailure
  case object SeatsAlreadyBooked extends ReservationFailure
  case object UnknownSeats extends ReservationFailure
  case object DuplicateSeats extends ReservationFailure

  sealed trait ReleaseFailure
  case object UnknownBooking extends ReleaseFailure
}
