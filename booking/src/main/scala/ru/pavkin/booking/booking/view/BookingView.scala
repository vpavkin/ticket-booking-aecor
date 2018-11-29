package ru.pavkin.booking.booking.view

import java.time.Instant

import io.circe._
import io.circe.generic.semiauto._
import ru.pavkin.booking.common.models.{BookingKey, _}
import ru.pavkin.booking.common.json.AnyValCoders._

case class BookingView(
  bookingId: BookingKey,
  clientId: ClientId,
  concertId: ConcertId,
  seats: List[Seat],
  tickets: List[Ticket],
  status: BookingStatus,
  confirmedAt: Option[Instant],
  version: Long)

object BookingView {
  implicit val decoder: Decoder[BookingView] = deriveDecoder
  implicit val encoder: Encoder[BookingView] = deriveEncoder
}
