package ru.pavkin.booking.booking.process

import aecor.data._
import aecor.distributedprocessing.DistributedProcessing
import cats.effect.ConcurrentEffect
import cats.implicits._
import ru.pavkin.booking.booking.entity.{ BookingEvent, BookingPlaced }
import ru.pavkin.booking.booking.process.BookingConfirmationProcessWiring.EventSource
import ru.pavkin.booking.common.models.BookingKey
import ru.pavkin.booking.common.streaming.Fs2Process

class BookingConfirmationProcessWiring[F[_]: ConcurrentEffect](
  eventSource: (EventTag, ConsumerId) => EventSource[F],
  tagging: Tagging[BookingKey],
  process: (BookingKey, BookingPlaced) => F[Unit]
) {

  val consumerId = ConsumerId("BookingConfirmationProcess")

  def tagProcess(tag: EventTag): fs2.Stream[F, Unit] =
    eventSource(tag, consumerId)
      .collect(Committable.collector { case EntityEvent(k, _, e: BookingPlaced) => k -> e })
      .evalMap(c => process(c.value._1, c.value._2) >> c.commit)

  def processes: List[DistributedProcessing.Process[F]] =
    tagging.tags.map(tag => Fs2Process(tagProcess(tag)))

}

object BookingConfirmationProcessWiring {
  type EventSource[F[_]] = fs2.Stream[F, Committable[F, EntityEvent[BookingKey, BookingEvent]]]
}
