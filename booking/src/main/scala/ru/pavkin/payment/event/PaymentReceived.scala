package ru.pavkin.payment.event
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import ru.pavkin.booking.common.models.{ BookingKey, ClientId, PaymentId }
import ru.pavkin.booking.common.json.AnyValCoders._

case class PaymentReceived(clientId: ClientId, paymentId: PaymentId, bookingId: Option[BookingKey])

object PaymentReceived {
  implicit val encoder: Encoder[PaymentReceived] = deriveEncoder
  implicit val decoder: Decoder[PaymentReceived] = deriveDecoder
}
