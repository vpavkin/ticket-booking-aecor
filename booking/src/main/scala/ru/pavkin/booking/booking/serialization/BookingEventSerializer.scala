package ru.pavkin.booking.booking.serialization

import aecor.journal.postgres.PostgresEventJournal
import aecor.journal.postgres.PostgresEventJournal.Serializer.TypeHint
import cats.data.NonEmptyList
import enumeratum.EnumEntry
import ru.pavkin.booking.booking.entity._
import ru.pavkin.booking.booking.protobuf.msg
import enumeratum._
import cats.syntax.either._

import scala.collection.immutable

object BookingEventSerializer extends PostgresEventJournal.Serializer[BookingEvent] {

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

  def serialize(a: BookingEvent): (TypeHint, Array[Byte]) = a match {
    case BookingPlaced(clientId, concertId, seats) =>
      AA.entryName -> msg.BookingPlaced(clientId, concertId, seats.toList).toByteArray
    case BookingConfirmed(tickets) =>
      AB.entryName -> msg.BookingConfirmed(tickets.toList).toByteArray
    case BookingDenied(reason) =>
      AC.entryName -> msg.BookingDenied(reason).toByteArray
    case BookingCancelled(reason) =>
      AD.entryName -> msg.BookingCancelled(reason).toByteArray
    case BookingExpired() =>
      AE.entryName -> msg.BookingExpired().toByteArray
    case BookingPaid(paymentId) =>
      AF.entryName -> msg.BookingPaid(paymentId).toByteArray
    case BookingSettled() =>
      AG.entryName -> msg.BookingSettled().toByteArray
  }

  def deserialize(typeHint: TypeHint, bytes: Array[Byte]): Either[Throwable, BookingEvent] =
    Either.catchNonFatal(Hint.withName(typeHint) match {

      case Hint.AA =>
        val raw = msg.BookingPlaced.parseFrom(bytes)
        BookingPlaced(raw.clientId, raw.concertId, NonEmptyList.fromListUnsafe(raw.seats.toList))

      case Hint.AB =>
        val raw = msg.BookingConfirmed.parseFrom(bytes)
        BookingConfirmed(NonEmptyList.fromListUnsafe(raw.tickets.toList))

      case Hint.AC =>
        val raw = msg.BookingDenied.parseFrom(bytes)
        BookingDenied(raw.reason)

      case Hint.AD =>
        val raw = msg.BookingCancelled.parseFrom(bytes)
        BookingCancelled(raw.reason)

      case Hint.AE =>
        BookingExpired()

      case Hint.AF =>
        val raw = msg.BookingPaid.parseFrom(bytes)
        BookingPaid(raw.paymentId)

      case Hint.AG =>
        BookingSettled()

    })
}
