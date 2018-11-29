package ru.pavkin.booking.booking.endpoint

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import ru.pavkin.booking.booking.booking.Bookings
import ru.pavkin.booking.booking.entity.BookingCommandRejection
import ru.pavkin.booking.booking.view.{BookingView, BookingViewRepository}
import ru.pavkin.booking.common.models.{BookingKey, ClientId, ConcertId, Seat}

trait BookingEndpoint[F[_]] {
  def placeBooking(
    client: ClientId,
    concertId: ConcertId,
    seats: NonEmptyList[Seat]): F[Either[BookingCommandRejection, Unit]]
  def clientBookings(client: ClientId): F[List[BookingView]]
}

final class DefaultBookingEndpoint[F[_] : Sync](
  bookings: Bookings[F],
  bookingsView: BookingViewRepository[F])
  extends BookingEndpoint[F] {

  def placeBooking(
    client: ClientId,
    concertId: ConcertId,
    seats: NonEmptyList[Seat]): F[Either[BookingCommandRejection, Unit]] =
    for {
      id <- Sync[F].delay(UUID.randomUUID())
      result <- bookings(BookingKey(id.toString)).place(client, concertId, seats)
    } yield result

  def clientBookings(client: ClientId): F[List[BookingView]] =
    bookingsView.byClient(client)
}
