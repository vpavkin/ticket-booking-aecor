package ru.pavkin.booking.booking.entity

import aecor.macros.boopickleWireProtocol
import cats.data.NonEmptyList
import cats.tagless.autoFunctorK
import ru.pavkin.booking.common.models._

@autoFunctorK(false)
@boopickleWireProtocol
trait Booking[F[_]] {

  def place(client: ClientId, concert: ConcertId, seats: NonEmptyList[Seat]): F[Unit]
  def confirm(tickets: NonEmptyList[Ticket]): F[Unit]
  def deny(reason: String): F[Unit]
  def cancel(reason: String): F[Unit]
  def receivePayment(paymentId: PaymentId): F[Unit]

  def status: F[Option[BookingStatus]]
  def tickets: F[Option[NonEmptyList[Ticket]]]
}
