package ru.pavkin.booking.booking.process

import cats.Monad
import ru.pavkin.booking.booking.booking.Bookings
import ru.pavkin.booking.booking.entity.BookingPlaced
import ru.pavkin.booking.booking.service.TicketReservationService
import ru.pavkin.booking.common.models.BookingKey
import cats.implicits._
import io.chrisdavenport.log4cats.Logger

class BookingConfirmationProcess[F[_]: Monad](bookings: Bookings[F],
                                              reservationService: TicketReservationService[F],
                                              logger: Logger[F])
    extends ((BookingKey, BookingPlaced) => F[Unit]) {

  def apply(key: BookingKey, event: BookingPlaced): F[Unit] =
    for {
      reservation <- reservationService.reserve(key, event.concertId, event.seats)
      commandResult <- reservation.fold(
                        err => bookings(key).deny(err.toString),
                        reservation => bookings(key).confirm(reservation.tickets, reservation.expiresAt)
                      )
      res <- commandResult.fold(
              r => logger.error(s"Booking confirmation process got unexpected rejection for $key: $r"),
              _.pure[F]
            )
    } yield res
}
