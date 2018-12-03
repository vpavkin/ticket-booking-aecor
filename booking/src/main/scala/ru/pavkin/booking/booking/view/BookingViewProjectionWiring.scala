package ru.pavkin.booking.booking.view

import aecor.data._
import aecor.distributedprocessing.DistributedProcessing
import cats.effect.ConcurrentEffect
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import ru.pavkin.booking.booking.entity.{ BookingEvent, EventMetadata }
import ru.pavkin.booking.booking.view.BookingViewProjectionWiring.EventSource
import ru.pavkin.booking.common.models.BookingKey
import ru.pavkin.booking.common.streaming.Fs2Process
import ru.pavkin.booking.common.view.ProjectionFlow

class BookingViewProjectionWiring[F[_]](
  repo: BookingViewRepository[F],
  eventSource: (EventTag, ConsumerId) => EventSource[F],
  tagging: Tagging[BookingKey]
)(implicit F: ConcurrentEffect[F]) {

  val consumerId = ConsumerId("BookingViewProjection")

  val sink =
    ProjectionFlow(Slf4jLogger.unsafeCreate[F], new BookingViewProjection[F](repo))

  def tagProcess(tag: EventTag): fs2.Stream[F, Unit] =
    eventSource(tag, consumerId).through(sink)

  def processes: List[DistributedProcessing.Process[F]] =
    tagging.tags.map(tag => Fs2Process(tagProcess(tag)))
}

object BookingViewProjectionWiring {
  type EventSource[F[_]] =
    fs2.Stream[F, Committable[F, EntityEvent[BookingKey, Enriched[EventMetadata, BookingEvent]]]]

}
