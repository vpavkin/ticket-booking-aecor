package ru.pavkin.booking.booking.serialization

import aecor.data.Enriched
import aecor.journal.postgres.PostgresEventJournal
import aecor.journal.postgres.PostgresEventJournal.Serializer.TypeHint
import cats.data.NonEmptyList
import enumeratum.EnumEntry
import ru.pavkin.booking.booking.entity._
import ru.pavkin.booking.booking.protobuf.msg
import enumeratum._
import cats.syntax.either._

import scala.collection.immutable

object BookingEventSerializer
    extends PostgresEventJournal.Serializer[Enriched[EventMetadata, BookingEvent]] {

  sealed trait Hint extends EnumEntry
  object Hint extends Enum[Hint] {
    case object AA extends Hint
    case object AB extends Hint
    case object AC extends Hint
    case object AD extends Hint
    case object AE extends Hint
    case object AF extends Hint
    case object AG extends Hint
    def values: immutable.IndexedSeq[Hint] = findValues
  }

  import Hint._

  def serialize(a: Enriched[EventMetadata, BookingEvent]): (TypeHint, Array[Byte]) = a match {
    case Enriched(m, BookingPlaced(clientId, concertId, seats)) =>
      AA.entryName -> msg.BookingPlaced(clientId, concertId, seats.toList, m.timestamp).toByteArray
    case Enriched(m, BookingConfirmed(tickets, expiresAt)) =>
      AB.entryName -> msg.BookingConfirmed(tickets.toList, expiresAt, m.timestamp).toByteArray
    case Enriched(m, BookingDenied(reason)) =>
      AC.entryName -> msg.BookingDenied(reason, m.timestamp).toByteArray
    case Enriched(m, BookingCancelled(reason)) =>
      AD.entryName -> msg.BookingCancelled(reason, m.timestamp).toByteArray
    case Enriched(m, BookingExpired) =>
      AE.entryName -> msg.BookingExpired(m.timestamp).toByteArray
    case Enriched(m, BookingPaid(paymentId)) =>
      AF.entryName -> msg.BookingPaid(paymentId, m.timestamp).toByteArray
    case Enriched(m, BookingSettled) =>
      AG.entryName -> msg.BookingSettled(m.timestamp).toByteArray
  }

  def deserialize(typeHint: TypeHint,
                  bytes: Array[Byte]): Either[Throwable, Enriched[EventMetadata, BookingEvent]] =
    Either.catchNonFatal(Hint.withName(typeHint) match {

      case Hint.AA =>
        val raw = msg.BookingPlaced.parseFrom(bytes)
        Enriched(
          EventMetadata(raw.timestamp),
          BookingPlaced(raw.clientId, raw.concertId, NonEmptyList.fromListUnsafe(raw.seats.toList))
        )

      case Hint.AB =>
        val raw = msg.BookingConfirmed.parseFrom(bytes)
        Enriched(
          EventMetadata(raw.timestamp),
          BookingConfirmed(NonEmptyList.fromListUnsafe(raw.tickets.toList), raw.expiresAt)
        )

      case Hint.AC =>
        val raw = msg.BookingDenied.parseFrom(bytes)
        Enriched(EventMetadata(raw.timestamp), BookingDenied(raw.reason))

      case Hint.AD =>
        val raw = msg.BookingCancelled.parseFrom(bytes)
        Enriched(EventMetadata(raw.timestamp), BookingCancelled(raw.reason))

      case Hint.AE =>
        val raw = msg.BookingExpired.parseFrom(bytes)
        Enriched(EventMetadata(raw.timestamp), BookingExpired)

      case Hint.AF =>
        val raw = msg.BookingPaid.parseFrom(bytes)
        Enriched(EventMetadata(raw.timestamp), BookingPaid(raw.paymentId))

      case Hint.AG =>
        val raw = msg.BookingSettled.parseFrom(bytes)
        Enriched(EventMetadata(raw.timestamp), BookingSettled)

    })
}
