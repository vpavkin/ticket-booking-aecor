package ru.pavkin.booking.booking.endpoint
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class CancelBookingRequest(reason: String)

object CancelBookingRequest {
  implicit val decoder: Decoder[CancelBookingRequest] = deriveDecoder
}
