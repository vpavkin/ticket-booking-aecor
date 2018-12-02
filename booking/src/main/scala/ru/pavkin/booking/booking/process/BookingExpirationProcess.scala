package ru.pavkin.booking.booking.process

import java.time.Instant

import cats.effect.Sync
import cats.implicits._
import ru.pavkin.booking.booking.booking.Bookings
import ru.pavkin.booking.booking.view.BookingViewRepository

class BookingExpirationProcess[F[_]: Sync](bookings: Bookings[F],
                                            bookingView: BookingViewRepository[F])
    extends (Instant => F[Unit]) {

  def apply(now: Instant): F[Unit] =
    bookingView
      .expired(now)
      .evalMap(k => bookings(k).expire.void)
      .compile
      .drain

}
