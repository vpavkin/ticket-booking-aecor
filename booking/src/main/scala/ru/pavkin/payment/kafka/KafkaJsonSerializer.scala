package ru.pavkin.payment.kafka
import java.nio.charset.StandardCharsets
import java.util

import io.circe.parser._
import io.circe.Encoder
import org.apache.kafka.common.serialization.{ Deserializer, Serializer, StringSerializer }
import ru.pavkin.payment.event.PaymentReceived

class PaymentReceivedEventSerializer extends Serializer[PaymentReceived] {
  private val stringSerializer = new StringSerializer

  def configure(configs: util.Map[String, _], isKey: Boolean): Unit = ()

  def serialize(topic: String, data: PaymentReceived): Array[Byte] =
    stringSerializer.serialize(topic, Encoder[PaymentReceived].apply(data).noSpaces)

  def close(): Unit = ()
}

class PaymentReceivedEventDeserializer extends Deserializer[PaymentReceived] {
  def configure(configs: util.Map[String, _], isKey: Boolean): Unit = ()

  def close(): Unit = ()

  def deserialize(topic: String, data: Array[Byte]): PaymentReceived =
    if (data ne null)
      decode[PaymentReceived](new String(data, StandardCharsets.UTF_8)).fold(throw _, identity)
    else null

}
