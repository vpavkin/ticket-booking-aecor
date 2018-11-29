package ru.pavkin.booking.booking.view

import aecor.data.{EntityEvent, Folded}
import Folded.syntax._
import cats.Functor
import cats.implicits._
import ru.pavkin.booking.booking.entity._
import ru.pavkin.booking.common.models.BookingKey
import ru.pavkin.booking.common.view.Projection.Version
import ru.pavkin.booking.common.models.BookingStatus
import ru.pavkin.booking.common.view.Projection

class BookingViewProjection[F[_]: Functor](repo: BookingViewRepository[F])
    extends Projection[F, EntityEvent[BookingKey, BookingEvent], BookingView] {

  def fetchVersionAndState(
    event: EntityEvent[BookingKey, BookingEvent]
  ): F[(Version, Option[BookingView])] =
    repo
      .get(event.entityKey)
      .map(v => v.fold(Projection.initialVersion)(v => Version(v.version)) -> v)

  def saveNewVersion(s: BookingView, version: Version): F[Unit] =
    repo.set(s.copy(version = version.value))

  def applyEvent(
    s: Option[BookingView]
  )(event: EntityEvent[BookingKey, BookingEvent]): Folded[Option[BookingView]] = s match {
    case None =>
      event.payload match {
        case e: BookingPlaced =>
          Some(
            BookingView(
              event.entityKey,
              e.clientId,
              e.concertId,
              e.seats.toList,
              Nil,
              BookingStatus.AwaitingConfirmation,
              None,
              Projection.initialVersion.value
            )
          ).next
        case _ => impossible
      }

    case Some(s) =>
      event.payload match {
        case _: BookingPlaced => impossible
        // todo: confirmedAt
        case BookingConfirmed(tickets) =>
          s.copy(tickets = tickets.toList, status = BookingStatus.Confirmed, confirmedAt = None)
            .some
            .next
        case _: BookingDenied    => s.copy(status = BookingStatus.Denied).some.next
        case _: BookingCancelled => s.copy(status = BookingStatus.Canceled).some.next
        case _: BookingExpired   => s.copy(status = BookingStatus.Canceled).some.next
        case _: BookingPaid      => s.some.next
        case _: BookingSettled   => s.copy(status = BookingStatus.Settled).some.next
      }
  }
}
