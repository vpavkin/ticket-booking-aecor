package ru.pavkin.booking

import aecor.data.{ Enriched, TagConsumer }
import aecor.journal.postgres.{ Offset, PostgresEventJournal, PostgresOffsetStore }
import aecor.runtime.KeyValueStore
import cats.effect._
import cats.implicits._
import cats.temp.par._
import doobie.implicits._
import doobie.util.transactor.Transactor
import ru.pavkin.booking.booking.entity.EventsourcedBooking._
import ru.pavkin.booking.booking.entity.{ BookingEvent, EventMetadata, EventsourcedBooking }
import ru.pavkin.booking.booking.serialization.BookingEventSerializer
import ru.pavkin.booking.booking.view.PostgresBookingViewRepository
import ru.pavkin.booking.common.models.BookingKey
import ru.pavkin.booking.common.postgres.PostgresTransactor
import ru.pavkin.booking.config.{ AppConfig, PostgresJournals }

final class PostgresWirings[F[_]: Async: Timer: Par] private (val transactor: Transactor[F],
                                                              val journals: PostgresJournals) {

  val offsetStoreCIO = PostgresOffsetStore("consumer_offset")
  val offsetStore: KeyValueStore[F, TagConsumer, Offset] = offsetStoreCIO.mapK(transactor.trans)

  val bookingsJournal =
    new PostgresEventJournal[F, BookingKey, Enriched[EventMetadata, BookingEvent]](
      transactor,
      journals.booking.tableName,
      EventsourcedBooking.tagging,
      BookingEventSerializer
    )

  // views

  val bookingViewRepo = new PostgresBookingViewRepository[F](transactor)
}

object PostgresWirings {
  def apply[F[_]: Async: Timer: Par: ContextShift](
    settings: AppConfig
  ): Resource[F, PostgresWirings[F]] =
    for {
      transactor <- PostgresTransactor.transactor[F](settings.postgres)
      wirings = new PostgresWirings(transactor, settings.postgresJournals)
      _ <- Resource.liftF(
            List(
              wirings.offsetStoreCIO.createTable.transact(transactor),
              wirings.bookingViewRepo.createTable,
              wirings.bookingsJournal.createTable,
            ).parSequence
          )
    } yield wirings
}
