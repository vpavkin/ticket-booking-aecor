package ru.pavkin.booking.booking.endpoint

import cats.effect.Effect
import cats.implicits._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import ru.pavkin.booking.common.models.{BookingKey, ClientId}

final class BookingRoutes[F[_]: Effect](ops: BookingEndpoint[F]) extends Http4sDsl[F] {

  implicit val placeBookingDecoder = jsonOf[F, PlaceBookingRequest]
  implicit val cancelBookingDecoder = jsonOf[F, CancelBookingRequest]

  private val placeBooking: HttpRoutes[F] = HttpRoutes.of {
    case r @ POST -> Root / userId / "bookings" =>
      r.as[PlaceBookingRequest]
        .flatMap(
          r =>
            ops.placeBooking(ClientId(userId), r.concertId, r.seats).flatMap {
              case Left(err) => BadRequest(err.toString)
              case Right(_)  => Ok()
          }
        )
  }

  private val cancelBooking: HttpRoutes[F] = HttpRoutes.of {
    case r @ DELETE -> Root / userId / "bookings" / bookingId =>
      r.as[CancelBookingRequest]
        .flatMap(
          r =>
            ops.cancelBooking(ClientId(userId), BookingKey(bookingId), r.reason).flatMap {
              case Left(err) => BadRequest(err.toString)
              case Right(_)  => Ok()
          }
        )
  }

  private val clientBookings: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root / userId / "bookings" =>
      ops.clientBookings(ClientId(userId)).flatMap { bookings =>
        Ok(bookings.asJson)
      }
  }

  val routes: HttpRoutes[F] =
    placeBooking <+> clientBookings <+> cancelBooking

}
