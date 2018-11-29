package ru.pavkin.booking.booking.service

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import ru.pavkin.booking.booking.service.BookingConfirmationService._
import ru.pavkin.booking.booking.service.StubConfirmationService.ConcertState
import ru.pavkin.booking.common.models._

class StubConfirmationService[F[_]](state: Ref[F, Map[ConcertId, ConcertState]])
    extends BookingConfirmationService[F] {
  def book(bookingId: BookingKey,
           concertId: ConcertId,
           seats: NonEmptyList[Seat]): F[Either[ConfirmationFailure, NonEmptyList[Ticket]]] =
    state.modify[Either[ConfirmationFailure, NonEmptyList[Ticket]]](
      concerts =>
        concerts.get(concertId) match {
          case None => concerts -> Left(UnknownSeats)
          case Some(concertState) =>
            concertState
              .book(bookingId, seats)
              .fold(e => concerts -> Left(e), {
                case (c, t) => concerts.updated(concertId, c) -> Right(t)
              })

      }
    )

  def release(bookingId: BookingKey): F[Either[ReleaseFailure, Unit]] =
    state.modify[Either[ReleaseFailure, Unit]](
      concerts =>
        Either
          .fromOption(concerts.find(_._2.bookedSeats.contains(bookingId)), UnknownBooking)
          .flatMap {
            case (concertId, concertState) =>
              concertState.release(bookingId).map(concertId -> _)
          } match {
          case Left(value)                  => concerts -> Left(value)
          case Right((concertId, newState)) => concerts.updated(concertId, newState) -> Right(())
      }
    )
}

object StubConfirmationService {

  def apply[F[_]: Sync](initial: Map[ConcertId, ConcertState]): F[StubConfirmationService[F]] =
    Ref.of(initial).map(new StubConfirmationService(_))

  case class ConcertState(prices: Map[Seat, Money],
                          availableSeats: Set[Seat],
                          bookedSeats: Map[BookingKey, NonEmptyList[Seat]]) {

    def book(
      bookingId: BookingKey,
      seats: NonEmptyList[Seat]
    ): Either[ConfirmationFailure, (ConcertState, NonEmptyList[Ticket])] =
      if (bookedSeats.contains(bookingId)) Left(SeatsAlreadyBooked)
      else if (!seats.forall(availableSeats)) Left(SeatsAlreadyBooked)
      else if (!seats.forall(prices.contains)) Left(UnknownSeats)
      else
        Right(
          copy(
            availableSeats = availableSeats.diff(seats.toList.toSet),
            bookedSeats = bookedSeats.updated(bookingId, seats)
          ) -> seats.map(s => Ticket(s, prices(s)))
        )

    def release(bookingId: BookingKey): Either[ReleaseFailure, ConcertState] =
      bookedSeats.get(bookingId) match {
        case Some(booked) =>
          Right(
            copy(
              availableSeats = availableSeats ++ booked.toList.toSet,
              bookedSeats = bookedSeats - bookingId
            )
          )
        case None => Left(UnknownBooking)
      }
  }

}
