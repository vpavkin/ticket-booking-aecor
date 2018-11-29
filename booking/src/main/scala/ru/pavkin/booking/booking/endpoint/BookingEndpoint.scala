package ru.pavkin.booking.booking.endpoint

import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import ru.pavkin.booking.booking.booking.Bookings
import ru.pavkin.booking.booking.entity.{ BookingCommandRejection, BookingNotFound }
import ru.pavkin.booking.booking.view.{ BookingView, BookingViewRepository }
import ru.pavkin.booking.common.models.{ BookingKey, ClientId, ConcertId, Seat }

trait BookingEndpoint[F[_]] {
  def placeBooking(client: ClientId,
                   concertId: ConcertId,
                   seats: NonEmptyList[Seat]): F[Either[BookingCommandRejection, Unit]]

  def cancelBooking(clientId: ClientId,
                    bookingId: BookingKey,
                    reason: String): F[Either[BookingCommandRejection, Unit]]

  def clientBookings(client: ClientId): F[List[BookingView]]

}

final class DefaultBookingEndpoint[F[_]](
  bookings: Bookings[F],
  bookingsView: BookingViewRepository[F]
)(implicit F: Sync[F])
    extends BookingEndpoint[F] {

  def placeBooking(client: ClientId,
                   concertId: ConcertId,
                   seats: NonEmptyList[Seat]): F[Either[BookingCommandRejection, Unit]] =
    for {
      id <- Sync[F].delay(UUID.randomUUID())
      result <- bookings(BookingKey(id.toString)).place(client, concertId, seats)
    } yield result

  def cancelBooking(clientId: ClientId,
                    bookingId: BookingKey,
                    reason: String): F[Either[BookingCommandRejection, Unit]] =
    bookingsView.get(bookingId).flatMap {
      case None                               => F.pure(Left(BookingNotFound))
      case Some(b) if b.clientId =!= clientId => F.pure(Left(BookingNotFound))
      case Some(_)                            => bookings(bookingId).cancel(reason)
    }

  def clientBookings(client: ClientId): F[List[BookingView]] =
    bookingsView.byClient(client)
}
