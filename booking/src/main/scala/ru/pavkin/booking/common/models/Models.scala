package ru.pavkin.booking.common.models

import java.util.UUID

import cats.Order
import cats.implicits._
import cats.kernel.Monoid

case class Money(amount: BigDecimal) extends AnyVal

object Money {
  implicit val monoid: Monoid[Money] = new Monoid[Money] {
    def empty: Money = Money(0)
    def combine(x: Money, y: Money): Money = Money(x.amount + y.amount)
  }
}

case class ClientId(value: UUID) extends AnyVal
case class ConcertId(value: UUID) extends AnyVal

case class Row(num: Int) extends AnyVal
case class SeatNumber(num: Int) extends AnyVal

case class Seat(row: Row, number: SeatNumber)
object Seat {
  implicit val order: Order[Seat] = Order.by(s => (s.row.num, s.number.num))
}

case class Ticket(seat: Seat, price: Money)

case class PaymentId(value: UUID) extends AnyVal

sealed trait BookingStatus extends Product with Serializable

object BookingStatus {
  case object AwaitingConfirmation extends BookingStatus
  case object Confirmed extends BookingStatus
  case object Denied extends BookingStatus
  case object Canceled extends BookingStatus
  case object Settled extends BookingStatus
}
