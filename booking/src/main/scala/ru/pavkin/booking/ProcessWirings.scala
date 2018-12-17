package ru.pavkin.booking

import aecor.data._
import aecor.distributedprocessing.DistributedProcessing
import aecor.journal.postgres.Offset
import akka.actor.ActorSystem
import cats.effect.{ Clock, ConcurrentEffect, Timer }
import cats.implicits._
import cats.temp.par._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import ru.pavkin.booking.booking.entity.{ BookingEvent, EventMetadata, EventsourcedBooking }
import ru.pavkin.booking.booking.process.{ BookingPaymentProcess, _ }
import ru.pavkin.booking.booking.view.BookingViewProjectionWiring
import ru.pavkin.booking.common.models.BookingKey

import scala.concurrent.duration._

final class ProcessWirings[F[_]: Timer: ConcurrentEffect: Par](system: ActorSystem,
                                                               clock: Clock[F],
                                                               postgresWirings: PostgresWirings[F],
                                                               kafkaWirings: KafkaWirings[F],
                                                               serviceWirings: ServiceWirings[F],
                                                               entityWirings: EntityWirings[F]) {

  import serviceWirings._
  import postgresWirings._
  import kafkaWirings._
  import entityWirings._

  val distributedProcessing = DistributedProcessing(system)

  val bookingQueries =
    bookingsJournal.queries(journals.booking.pollingInterval).withOffsetStore(offsetStore)

  def bookingEvents(
    eventTag: EventTag,
    consumerId: ConsumerId
  ): fs2.Stream[F, Committable[F,
                               (Offset,
                                EntityEvent[BookingKey, Enriched[EventMetadata, BookingEvent]])]] =
    fs2.Stream.force(bookingQueries.eventsByTag(eventTag, consumerId))

  val bookingViewProjection = new BookingViewProjectionWiring(
    bookingViewRepo,
    bookingEvents(_, _).map(_.map(_._2)),
    EventsourcedBooking.tagging
  )

  val bookingConfirmationProcess =
    new BookingConfirmationProcess(
      bookings,
      confirmationService,
      Slf4jLogger.unsafeFromName("BookingConfirmationProcess")
    )

  val bookingConfirmationProcessWiring =
    new BookingConfirmationProcessWiring(
      bookingEvents(_, _).map(_.map(_._2.map(_.event))),
      EventsourcedBooking.tagging,
      bookingConfirmationProcess
    )

  val bookingExpirationProcess = new BookingExpirationProcess(bookings, bookingViewRepo)

  val bookingExpirationProcessWiring =
    new BookingExpirationProcessWiring(clock, frequency = 30.seconds, bookingExpirationProcess)

  val bookingPaymentProcess =
    new BookingPaymentProcess(bookings, Slf4jLogger.unsafeFromName("BookingPaymentProcess"))

  val bookingPaymentProcessWiring =
    new BookingPaymentProcessWiring(paymentReceivedEventStream, bookingPaymentProcess)

  // Launcher
  val launchProcesses: F[List[DistributedProcessing.KillSwitch[F]]] =
    List(
      "BookingViewProjectionProcessing" -> bookingViewProjection.processes,
      "BookingConfirmationProcessing" -> bookingConfirmationProcessWiring.processes,
      "BookingExpirationProcessing" -> bookingExpirationProcessWiring.processes,
      "BookingPaymentProcessing" -> bookingPaymentProcessWiring.processes
    ).parTraverse {
      case (name, processes) => distributedProcessing.start(name, processes)
    }

}
