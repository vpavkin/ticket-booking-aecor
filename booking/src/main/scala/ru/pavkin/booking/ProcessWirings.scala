package ru.pavkin.booking

import aecor.distributedprocessing.DistributedProcessing
import akka.actor.ActorSystem
import cats.effect.ConcurrentEffect
import cats.implicits._
import cats.temp.par._
import ru.pavkin.booking.booking.entity.EventsourcedBooking
import ru.pavkin.booking.booking.view.BookingViewProjectionWiring

import scala.concurrent.duration.{Duration => _}

final class ProcessWirings[F[_]: ConcurrentEffect: Par](system: ActorSystem,
                                                        postgresWirings: PostgresWirings[F]) {

  import postgresWirings._

  val distributedProcessing = DistributedProcessing(system)

  val bookingViewProjection = new BookingViewProjectionWiring[F](
    bookingViewRepo,
    bookingsJournal,
    offsetStore,
    EventsourcedBooking.tagging
  )

  // Launcher

  val launchProcesses: F[List[DistributedProcessing.KillSwitch[F]]] =
    List("BookingViewProjectionProcessing" -> bookingViewProjection.processes).parTraverse {
      case (name, processes) => distributedProcessing.start(name, processes)
    }

}
