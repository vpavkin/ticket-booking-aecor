package ru.pavkin.booking.booking.process

import cats.Monad
import ru.pavkin.booking.booking.booking.Bookings
import ru.pavkin.booking.booking.entity.BookingPlaced
import ru.pavkin.booking.booking.service.BookingConfirmationService
import ru.pavkin.booking.common.models.BookingKey
import cats.implicits._
import io.chrisdavenport.log4cats.Logger

class BookingConfirmationProcess[F[_]: Monad](bookings: Bookings[F],
                                              confirmationService: BookingConfirmationService[F],
                                              logger: Logger[F])
    extends ((BookingKey, BookingPlaced) => F[Unit]) {

  def apply(key: BookingKey, event: BookingPlaced): F[Unit] =
    confirmationService
      .book(key, event.concertId, event.seats)
      .flatMap {
        case Left(error)    => bookings(key).deny(error.toString)
        case Right(tickets) => bookings(key).confirm(tickets)
      }
      .flatMap(
        _.fold(
          r => logger.error(s"Booking confirmation process got unexpected rejection for $key: $r"),
          _.pure[F]
        )
      )
}
