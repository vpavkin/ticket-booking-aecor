package ru.pavkin.booking

import java.time.Instant
import java.util.concurrent.TimeUnit

import aecor.data.{ EitherK, Enriched }
import aecor.runtime.akkapersistence.serialization.{
  DecodingFailure,
  PersistentDecoder,
  PersistentEncoder,
  PersistentRepr
}
import aecor.runtime.akkapersistence.{ AkkaPersistenceRuntime, CassandraJournalAdapter }
import akka.actor.ActorSystem
import cats.effect._
import cats.implicits._
import ru.pavkin.booking.common.models.BookingKey
import ru.pavkin.booking.booking.entity._
import ru.pavkin.booking.booking.entity.BookingWireCodecs._
import ru.pavkin.booking.booking.serialization.BookingEventSerializer
import ru.pavkin.booking.common.effect.TimedOutBehaviour

import scala.concurrent.duration._

/**
  * This is an example of wiring an AkkaPersistenceRuntime entity deployment.
  * It's only a demo and is not used in the application.
  *
  * For this deployment to work properly, you'll need to configure akka-persistence-cassandra plugin properly
  */
final class AkkaPersistenceRuntimeWirings[F[_]](
  val bookings: BookingKey => EitherK[Booking, BookingCommandRejection, F]
)

object AkkaPersistenceRuntimeWirings {
  def apply[F[_]: ConcurrentEffect: Timer](system: ActorSystem,
                                           clock: Clock[F]): F[AkkaPersistenceRuntimeWirings[F]] = {

    val journalAdapter = CassandraJournalAdapter(system)
    val runtime = AkkaPersistenceRuntime(system, journalAdapter)

    implicit val eventEncoder: PersistentEncoder[Enriched[EventMetadata, BookingEvent]] =
      PersistentEncoder.instance { evt =>
        val (manifest, bytes) = BookingEventSerializer.serialize(evt)
        PersistentRepr(manifest, bytes)
      }

    implicit val eventDecoder: PersistentDecoder[Enriched[EventMetadata, BookingEvent]] =
      PersistentDecoder.instance { repr =>
        BookingEventSerializer
          .deserialize(repr.manifest, repr.payload)
          .leftMap(ex => DecodingFailure(ex.getMessage, Some(ex)))
      }

    val generateTimestamp: F[EventMetadata] =
      clock.realTime(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli).map(EventMetadata)

    val bookingsBehavior =
      TimedOutBehaviour(
        EventsourcedBooking.behavior[F](clock).enrich[EventMetadata](generateTimestamp)
      )(2.seconds)

    val bookings: F[BookingKey => EitherK[Booking, BookingCommandRejection, F]] = runtime
      .deploy(EventsourcedBooking.entityName, bookingsBehavior, EventsourcedBooking.tagging)

    bookings.map(new AkkaPersistenceRuntimeWirings(_))
  }
}
