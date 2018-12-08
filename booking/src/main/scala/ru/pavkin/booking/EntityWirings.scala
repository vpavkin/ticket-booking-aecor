package ru.pavkin.booking

import java.time.Instant
import java.util.concurrent.TimeUnit

import aecor.data.EitherK
import aecor.runtime.Eventsourced
import aecor.runtime.akkageneric.GenericAkkaRuntime
import akka.actor.ActorSystem
import cats.effect._
import cats.implicits._
import ru.pavkin.booking.booking.booking.Bookings
import ru.pavkin.booking.common.models.BookingKey
import ru.pavkin.booking.booking.entity.{Booking, BookingCommandRejection, EventMetadata, EventsourcedBooking}
import ru.pavkin.booking.booking.entity.BookingWireCodecs._
import ru.pavkin.booking.common.effect.TimedOutBehaviour

import scala.concurrent.duration._
final class EntityWirings[F[_]](val bookings: Bookings[F])

object EntityWirings {
  def apply[F[_]: ConcurrentEffect: Timer](
    system: ActorSystem,
    clock: Clock[F],
    postgresWirings: PostgresWirings[F]
  ): F[EntityWirings[F]] = {
    val genericAkkaRuntime = GenericAkkaRuntime(system)

    val generateTimestamp: F[EventMetadata] =
      clock.realTime(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli).map(EventMetadata)

    val bookingsBehavior =
      TimedOutBehaviour(
        EventsourcedBooking.behavior[F](clock).enrich[EventMetadata](generateTimestamp)
      )(2.seconds)

    val bookings: F[Bookings[F]] = genericAkkaRuntime
      .runBehavior[BookingKey, EitherK[Booking, BookingCommandRejection, ?[_]], F](
        EventsourcedBooking.entityName,
        Eventsourced(bookingsBehavior, postgresWirings.bookingsJournal)
      )
      .map(Eventsourced.Entities.fromEitherK(_))

    bookings.map(new EntityWirings(_))
  }

}
