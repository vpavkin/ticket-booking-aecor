package ru.pavkin.booking.booking.view

import java.time.Instant

import ru.pavkin.booking.common.models.{ BookingKey, ClientId }

trait BookingViewRepository[F[_]] {
  def get(bookingId: BookingKey): F[Option[BookingView]]
  def set(view: BookingView): F[Unit]

  def byClient(clientId: ClientId): F[List[BookingView]]

  def expired(now: Instant): fs2.Stream[F, BookingKey]
}
