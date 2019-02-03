package ru.pavkin.booking.booking.entity

import java.time.Instant

import aecor.macros.boopickleWireProtocol
import cats.data.NonEmptyList
import cats.tagless.autoFunctorK
import ru.pavkin.booking.common.models._
import boopickle.Default._
import BookingWireCodecs._

@autoFunctorK(false)
@boopickleWireProtocol
trait Booking[F[_]] {

  def place(client: ClientId, concert: ConcertId, seats: NonEmptyList[Seat]): F[Unit]
  def confirm(tickets: NonEmptyList[Ticket], expiresAt: Option[Instant]): F[Unit]
  def deny(reason: String): F[Unit]
  def cancel(reason: String): F[Unit]
  def receivePayment(paymentId: PaymentId): F[Unit]
  def expire: F[Unit]
  def status: F[BookingStatus]
}

object Booking
