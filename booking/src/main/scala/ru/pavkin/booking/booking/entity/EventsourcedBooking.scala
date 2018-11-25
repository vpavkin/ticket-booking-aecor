package ru.pavkin.booking.booking.entity

import java.util.UUID

import aecor.data._
import aecor.encoding.{KeyDecoder, KeyEncoder}
import aecor.{MonadAction, MonadActionReject}
import cats.Monad
import cats.data.EitherT._
import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all._
import ru.pavkin.booking.common.models.BookingStatus._
import ru.pavkin.booking.common.models._

class EventsourcedBooking[I[_]](
  implicit I: MonadActionReject[I, Option[BookingState], BookingEvent, BookingCommandRejection]
) extends Booking[I] {

  import I._

  val ignore: I[Unit] = unit

  val readStatus: I[BookingStatus] = read.flatMap {
    case Some(s) => pure(s.status)
    case _       => reject(BookingNotFound)
  }

  def place(client: ClientId, concert: ConcertId, seats: NonEmptyList[Seat]): I[Unit] =
    read.flatMap {
      case Some(_) => reject(BookingAlreadyExists)
      case None =>
        if (seats.distinct =!= seats) reject(DuplicateSeats)
        else if (seats.size > 10) reject(TooManySeats)
        else append(BookingPlaced(client, concert, seats))
    }

  def confirm(tickets: NonEmptyList[Ticket]): I[Unit] =
    readStatus.flatMap {
      case AwaitingConfirmation =>
        append(BookingConfirmed(tickets)) >>
          whenA(tickets.foldMap(_.price).amount <= 0)(append(BookingSettled()))

      case Confirmed | Settled => ignore
      case Denied              => reject(BookingIsDenied)
      case Canceled            => reject(BookingIsAlreadyCanceled)
    }

  def deny(reason: String): I[Unit] =
    readStatus.flatMap {
      case AwaitingConfirmation =>
        append(BookingDenied(reason))
      case Denied              => ignore
      case Confirmed | Settled => reject(BookingIsAlreadyConfirmed)
      case Canceled            => reject(BookingIsAlreadyCanceled)
    }

  def cancel(reason: String): I[Unit] =
    readStatus.flatMap {
      case AwaitingConfirmation | Confirmed =>
        append(BookingCancelled(reason))
      case Canceled | Denied => ignore
      case Settled           => reject(BookingIsAlreadySettled)
    }

  def receivePayment(paymentId: PaymentId): I[Unit] =
    readStatus.flatMap {
      case AwaitingConfirmation        => reject(BookingIsNotConfirmed)
      case Canceled | Denied | Settled => reject(BookingIsAlreadySettled)
      case Confirmed                   => append(BookingPaid(paymentId))
    }

  def status: I[Option[BookingStatus]] = read.map(_.map(_.status))
  def tickets: I[Option[NonEmptyList[Ticket]]] = read.map(_.flatMap(_.tickets))
}

object EventsourcedBooking {

  case class BookingKey(value: UUID) extends AnyVal

  implicit val keyEncoder: KeyEncoder[BookingKey] = KeyEncoder.encodeKeyUUID.contramap(_.value)

  implicit val keyDecoder: KeyDecoder[BookingKey] = KeyDecoder.decodeKeyUUID.map(BookingKey)

  implicitly[MonadAction[ActionT[IO, Option[BookingState], BookingEvent, ?],
                         Option[BookingState],
                         BookingEvent]]

  def behavior[F[_]: Monad]: EventsourcedBehavior[
    EitherK[Booking, BookingCommandRejection, ?[_]],
    F,
    Option[BookingState],
    BookingEvent
  ] =
    EventsourcedBehavior
      .optionalRejectable(
        new EventsourcedBooking(),
        BookingState.init,
        _.handleEvent(_)
      )

  val entityName: String = "Booking"
  val entityTag: EventTag = EventTag(entityName)
  val tagging: Tagging.Partitioned[BookingKey] = Tagging.partitioned(20)(entityTag)

}
