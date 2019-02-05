package ru.pavkin.booking

import cats.effect.{ Clock, Sync }
import cats.syntax.functor._
import ru.pavkin.booking.booking.service.{ TicketReservationService, StubConfirmationService }

final class ServiceWirings[F[_]: Sync](val confirmationService: TicketReservationService[F])

object ServiceWirings {

  def apply[F[_]: Sync](clock: Clock[F]): F[ServiceWirings[F]] =
    StubConfirmationService[F](clock, ConcertData.concertData).map(new ServiceWirings(_))
}
