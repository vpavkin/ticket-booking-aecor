package ru.pavkin.booking.booking.endpoint

import cats.data.NonEmptyList
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import ru.pavkin.booking.common.models.{ConcertId, Seat}
import ru.pavkin.booking.common.json.AnyValCoders._

case class PlaceBookingRequest(concertId: ConcertId, seats: NonEmptyList[Seat])

object PlaceBookingRequest {
  implicit val decoder: Decoder[PlaceBookingRequest] = deriveDecoder
}
