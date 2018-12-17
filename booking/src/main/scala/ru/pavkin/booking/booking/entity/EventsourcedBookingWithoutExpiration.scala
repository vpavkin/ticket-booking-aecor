package ru.pavkin.booking.booking.entity

import java.time.Instant

import aecor.MonadActionReject
import aecor.data._
import cats.Monad
import cats.data.EitherT._
import cats.data.NonEmptyList
import cats.syntax.all._
import ru.pavkin.booking.common.models.BookingStatus._
import ru.pavkin.booking.common.models._

// Just an example, isn't uses
class EventsourcedBookingWithoutExpiration[F[_]](
  implicit F: MonadActionReject[F, Option[BookingState], BookingEvent, BookingCommandRejection]
) extends Booking[F] {

  import F._

  val ignore: F[Unit] = unit

  def place(client: ClientId, concert: ConcertId, seats: NonEmptyList[Seat]): F[Unit] =
    read.flatMap {
      case Some(_) => reject(BookingAlreadyExists)
      case None =>
        if (seats.distinct =!= seats) reject(DuplicateSeats)
        else if (seats.size > 10) reject(TooManySeats)
        else append(BookingPlaced(client, concert, seats))
    }

  def confirm(tickets: NonEmptyList[Ticket], expiresAt: Option[Instant]): F[Unit] =
    status.flatMap {
      case AwaitingConfirmation =>
        append(BookingConfirmed(tickets, null)) >>
          whenA(tickets.foldMap(_.price).amount <= 0)(append(BookingSettled))

      case Confirmed | Settled => ignore
      case Denied              => reject(BookingIsDenied)
      case Canceled            => reject(BookingIsAlreadyCanceled)
    }

  def expire: F[Unit] = ???

  def deny(reason: String): F[Unit] =
    status.flatMap {
      case AwaitingConfirmation =>
        append(BookingDenied(reason))
      case Denied              => ignore
      case Confirmed | Settled => reject(BookingIsAlreadyConfirmed)
      case Canceled            => reject(BookingIsAlreadyCanceled)
    }

  def cancel(reason: String): F[Unit] =
    status.flatMap {
      case AwaitingConfirmation | Confirmed =>
        append(BookingCancelled(reason))
      case Canceled | Denied => ignore
      case Settled           => reject(BookingIsAlreadySettled)
    }

  def receivePayment(paymentId: PaymentId): F[Unit] =
    status.flatMap {
      case AwaitingConfirmation        => reject(BookingIsNotConfirmed)
      case Canceled | Denied | Settled => reject(BookingIsAlreadySettled)
      case Confirmed                   => append(BookingPaid(paymentId)) >> append(BookingSettled)
    }

  def status: F[BookingStatus] = read.flatMap {
    case Some(s) => pure(s.status)
    case _       => reject(BookingNotFound)
  }

  def tickets: F[Option[NonEmptyList[Ticket]]] = read.map(_.flatMap(_.tickets))
}

object EventsourcedBookingWithoutExpiration {

  def behavior[F[_]: Monad]: EventsourcedBehavior[
    EitherK[Booking, BookingCommandRejection, ?[_]],
    F,
    Option[BookingState],
    BookingEvent
  ] =
    EventsourcedBehavior
      .optionalRejectable(
        new EventsourcedBookingWithoutExpiration(),
        BookingState.init,
        _.handleEvent(_)
      )
}
