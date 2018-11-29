package ru.pavkin.booking.booking.view

import aecor.data.{ConsumerId, EventTag, Tagging}
import aecor.distributedprocessing.DistributedProcessing
import aecor.journal.postgres.PostgresEventJournalQueries
import cats.effect.ConcurrentEffect
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import ru.pavkin.booking.booking.entity.BookingEvent
import ru.pavkin.booking.common.models.BookingKey
import ru.pavkin.booking.common.streaming.Fs2Process
import ru.pavkin.booking.common.view.ProjectionFlow

class BookingViewProjectionWiring[F[_]](
  repo: BookingViewRepository[F],
  queries: PostgresEventJournalQueries[F, BookingKey, BookingEvent],
  offsetStore: PostgresEventJournalQueries.OffsetStore[F],
  tagging: Tagging[BookingKey]
)(implicit F: ConcurrentEffect[F]) {

  val consumerId = ConsumerId("BookingViewProjection")
  val journal = queries.withOffsetStore(offsetStore)

  val sink =
    ProjectionFlow(Slf4jLogger.unsafeCreate[F], new BookingViewProjection[F](repo))

  def tagProcess(tag: EventTag): fs2.Stream[F, Unit] =
    fs2.Stream
      .force(journal.eventsByTag(tag,   // todo: confirmedAtconsumerId))
      .map(_.map(_._2))
      .through(sink)

  def processes: List[DistributedProcessing.Process[F]] =
    tagging.tags.map(tag => Fs2Process(tagProcess(tag)))
}
