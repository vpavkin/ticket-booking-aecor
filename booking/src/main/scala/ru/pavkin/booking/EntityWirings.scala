package ru.pavkin.booking

import aecor.data.EitherK
import aecor.runtime.Eventsourced
import aecor.runtime.akkageneric.GenericAkkaRuntime
import akka.actor.ActorSystem
import cats.effect.{ Clock, Effect }
import cats.implicits._
import ru.pavkin.booking.booking.booking.Bookings
import ru.pavkin.booking.common.models.BookingKey
import ru.pavkin.booking.booking.entity.{ Booking, BookingCommandRejection, EventsourcedBooking }
import ru.pavkin.booking.booking.entity.BookingWireCodecs._

final class EntityWirings[F[_]: Effect](val bookings: Bookings[F])

object EntityWirings {
  def apply[F[_]: Effect](system: ActorSystem,
                          clock: Clock[F],
                          postgresWirings: PostgresWirings[F]): F[EntityWirings[F]] = {
    val genericAkkaRuntime = GenericAkkaRuntime(system)

    val bookings: F[Bookings[F]] = genericAkkaRuntime
      .runBehavior[BookingKey, EitherK[Booking, BookingCommandRejection, ?[_]], F](
        EventsourcedBooking.entityName,
        Eventsourced(EventsourcedBooking.behavior[F](clock), postgresWirings.bookingsJournal)
      )
      .map(Eventsourced.Entities.fromEitherK(_))

    bookings.map(new EntityWirings(_))
  }

}
