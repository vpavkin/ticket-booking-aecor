package ru.pavkin.booking

import aecor.data.{ Committable, ConsumerId }
import cats.data.NonEmptyList
import cats.effect._
import fs2.kafka.{ AutoOffsetReset, ConsumerSettings, _ }
import org.apache.kafka.common.serialization.StringDeserializer
import ru.pavkin.payment.event.PaymentReceived
import ru.pavkin.payment.kafka.PaymentReceivedEventDeserializer

import scala.concurrent.ExecutionContext

final class KafkaWirings[F[_]](
  val paymentReceivedEventStream: ConsumerId => fs2.Stream[F, Committable[F, PaymentReceived]]
)

object KafkaWirings {

  def apply[F[_]: ConcurrentEffect: ContextShift: Timer]: KafkaWirings[F] = {

    def paymentReceivedEventStream(
      consumerId: ConsumerId
    ): fs2.Stream[F, Committable[F, PaymentReceived]] =
      for {
        executionContext <- consumerExecutionContextStream[F]
        settings = bookingPaymentProcessSourceSettings(executionContext).withGroupId(
          consumerId.value
        )
        consumer <- consumerStream[F].using(settings)
        _ <- consumer.subscribe(paymentReceivedTopic)
        stream <- consumer.stream.map(
                   m => Committable(m.committableOffset.commit, m.record.value())
                 )
      } yield stream

    new KafkaWirings[F](paymentReceivedEventStream)
  }

  def bookingPaymentProcessSourceSettings(
    ec: ExecutionContext
  ): ConsumerSettings[String, PaymentReceived] =
    ConsumerSettings(
      keyDeserializer = new StringDeserializer,
      valueDeserializer = new PaymentReceivedEventDeserializer,
      executionContext = ec
    ).withAutoOffsetReset(AutoOffsetReset.Earliest).withBootstrapServers("0.0.0.0:9092")

  val paymentReceivedTopic: NonEmptyList[String] = NonEmptyList.one("PaymentReceived")
}
