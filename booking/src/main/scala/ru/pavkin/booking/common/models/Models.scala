package ru.pavkin.booking.common.models

import cats.Order
import cats.implicits._
import cats.kernel.Monoid
import enumeratum._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import ru.pavkin.booking.common.json.AnyValCoders._

import scala.collection.immutable

case class Money(amount: BigDecimal) extends AnyVal

object Money {
  implicit val monoid: Monoid[Money] = new Monoid[Money] {
    def empty: Money = Money(0)
    def combine(x: Money, y: Money): Money = Money(x.amount + y.amount)
  }
}

case class BookingKey(value: String) extends AnyVal

case class ClientId(value: String) extends AnyVal
case class ConcertId(value: String) extends AnyVal

case class Row(num: Int) extends AnyVal
case class SeatNumber(num: Int) extends AnyVal

case class Seat(row: Row, number: SeatNumber)

object Seat {
  implicit val order: Order[Seat] = Order.by(s => (s.row.num, s.number.num))
  implicit val decoder: Decoder[Seat] = deriveDecoder
  implicit val encoder: Encoder[Seat] = deriveEncoder
}

case class Ticket(seat: Seat, price: Money)
object Ticket {
  implicit val decoder: Decoder[Ticket] = deriveDecoder
  implicit val encoder: Encoder[Ticket] = deriveEncoder
}
case class PaymentId(value: String) extends AnyVal

sealed trait BookingStatus extends EnumEntry

object BookingStatus extends Enum[BookingStatus] with CirceEnum[BookingStatus] {
  case object AwaitingConfirmation extends BookingStatus
  case object Confirmed extends BookingStatus
  case object Denied extends BookingStatus
  case object Canceled extends BookingStatus
  case object Settled extends BookingStatus

  def values: immutable.IndexedSeq[BookingStatus] = findValues
}
