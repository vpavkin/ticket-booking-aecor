package ru.pavkin.booking

import aecor.data.{ Committable, ConsumerId, EntityEvent, EventTag }
import aecor.distributedprocessing.DistributedProcessing
import aecor.journal.postgres.Offset
import akka.actor.ActorSystem
import cats.effect.ConcurrentEffect
import cats.implicits._
import cats.temp.par._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import ru.pavkin.booking.booking.entity.{ BookingEvent, EventsourcedBooking }
import ru.pavkin.booking.booking.process.{
  BookingConfirmationProcess,
  BookingConfirmationProcessWiring
}
import ru.pavkin.booking.booking.view.BookingViewProjectionWiring
import ru.pavkin.booking.common.models.BookingKey

final class ProcessWirings[F[_]: ConcurrentEffect: Par](system: ActorSystem,
                                                        postgresWirings: PostgresWirings[F],
                                                        serviceWirings: ServiceWirings[F],
                                                        entityWirings: EntityWirings[F]) {

  import serviceWirings._
  import postgresWirings._
  import entityWirings._

  val distributedProcessing = DistributedProcessing(system)

  def bookingEvents(
    eventTag: EventTag,
    consumerId: ConsumerId
  ): fs2.Stream[F, Committable[F, (Offset, EntityEvent[BookingKey, BookingEvent])]] =
    fs2.Stream.force(bookingsJournal.withOffsetStore(offsetStore).eventsByTag(eventTag, consumerId))

  val bookingViewProjection = new BookingViewProjectionWiring[F](
    bookingViewRepo,
    bookingEvents(_, _).map(_.map(_._2)),
    EventsourcedBooking.tagging
  )

  val bookingConfirmationProcess =
    new BookingConfirmationProcess[F](bookings, confirmationService, Slf4jLogger.unsafeCreate[F])

  val bookingConfirmationProcessWiring =
    new BookingConfirmationProcessWiring[F](
      bookingEvents(_, _).map(_.map(_._2)),
      EventsourcedBooking.tagging,
      bookingConfirmationProcess
    )

  // Launcher
  val launchProcesses: F[List[DistributedProcessing.KillSwitch[F]]] =
    List(
      "BookingViewProjectionProcessing" -> bookingViewProjection.processes,
      "BookingConfirmationProcessing" -> bookingConfirmationProcessWiring.processes
    ).parTraverse {
      case (name, processes) => distributedProcessing.start(name, processes)
    }

}
