package ru.pavkin.booking.booking.view

import ru.pavkin.booking.common.models.{BookingKey, ClientId}

trait BookingViewRepository[F[_]] {
  def get(bookingId: BookingKey): F[Option[BookingView]]
  def byClient(clientId: ClientId): F[List[BookingView]]
  def set(view: BookingView): F[Unit]
}
