package ru.pavkin.booking.booking.view

import java.sql.Timestamp
import java.time.Instant

import cats.Monad
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.{Decoder, Encoder, Json}
import io.circe.parser._
import org.postgresql.util.PGobject
import ru.pavkin.booking.common.models._

class PostgresBookingViewRepository[F[_] : Monad](
  transactor: Transactor[F],
  tableName: String = "bookings")
  extends BookingViewRepository[F] {

  implicit val jsonMeta: Meta[Json] =
    Meta.Advanced
      .other[PGobject]("json")
      .timap[Json](a => parse(a.getValue).leftMap[Json](e => throw e).merge)(a => {
      val o = new PGobject
      o.setType("json")
      o.setValue(a.noSpaces)
      o
    })

  implicit val seatsMeta: Meta[List[Seat]] = jsonMeta.timap(
    j => Decoder[List[Seat]].decodeJson(j).right.get
  )(s => Encoder[List[Seat]].apply(s))

  implicit val ticketsMeta: Meta[List[Ticket]] = jsonMeta.timap(
    j => Decoder[List[Ticket]].decodeJson(j).right.get
  )(s => Encoder[List[Ticket]].apply(s))

  implicit val instantMeta: Meta[Instant] =
    Meta[Timestamp].timap(_.toInstant)(Timestamp.from)

  implicit val bookingStatusMeta: Meta[BookingStatus] =
    Meta[String].timap(BookingStatus.withName)(_.entryName)

  def get(bookingId: BookingKey): F[Option[BookingView]] =
    queryView(bookingId).option.transact(transactor)

  def byClient(clientId: ClientId): F[List[BookingView]] =
    queryForClient(clientId).to[List].transact(transactor)

  def set(view: BookingView): F[Unit] =
    Update[BookingView](setViewQuery).run(view).transact(transactor).void

  def createTable: F[Unit] = createTableQuery.transact(transactor).void

  private val setViewQuery =
    s"""INSERT INTO $tableName
    (booking_id, client_id, concert_id, seats, tickets, status, confirmed_at, version)
    VALUES (?,?,?,?,?,?,?,?)
    ON CONFLICT (booking_id)
    DO UPDATE SET
     tickets = EXCLUDED.tickets,
     status = EXCLUDED.status,
     version = EXCLUDED.version;"""

  private def queryView(bookingId: BookingKey) =
    (fr"SELECT * FROM " ++ Fragment.const(tableName) ++
      fr"WHERE booking_id = $bookingId;")
      .query[BookingView]

  private def queryForClient(clientId: ClientId) =
    (fr"SELECT * FROM " ++ Fragment.const(tableName) ++
      fr"WHERE client_id = $clientId;")
      .query[BookingView]

  private val createTableQuery = (fr"""
    CREATE TABLE IF NOT EXISTS """ ++ Fragment.const(tableName) ++
    fr""" (
    booking_id    text      NOT NULL PRIMARY KEY,
    client_id     text      NOT NULL,
    concert_id    text      NOT NULL,
    seats         json      NOT NULL,
    tickets       json      NOT NULL,
    status        text      NOT NULL,
    confirmed_at  timestamp,
    version       bigint    NOT NULL
    );
  """).update.run

}
