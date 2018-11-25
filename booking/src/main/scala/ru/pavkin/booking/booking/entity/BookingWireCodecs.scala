package ru.pavkin.booking.booking.entity

import java.time.Duration

import boopickle.Default.{compositePickler, _}
import ru.pavkin.booking.common.models.{ClientId, ConcertId, PaymentId}
import scodec.Codec

object BookingWireCodecs {

  implicit val clientIdPickler: boopickle.Pickler[ClientId] =
    boopickle.DefaultBasic.UUIDPickler.xmap(ClientId)(_.value)

  implicit val concertIdPickler: boopickle.Pickler[ConcertId] =
    boopickle.DefaultBasic.UUIDPickler.xmap(ConcertId)(_.value)

  implicit val paymentIdPickler: boopickle.Pickler[PaymentId] =
    boopickle.DefaultBasic.UUIDPickler.xmap(PaymentId)(_.value)

  implicit val durationPickler: boopickle.Pickler[Duration] =
    boopickle.DefaultBasic.longPickler.xmap(Duration.ofMillis)(_.toMillis)

  implicit val rejectionPickler: boopickle.Pickler[BookingCommandRejection] =
    compositePickler[BookingCommandRejection]
      .addConcreteType[BookingAlreadyExists.type]
      .addConcreteType[BookingNotFound.type]
      .addConcreteType[TooManySeats.type]
      .addConcreteType[DuplicateSeats.type]
      .addConcreteType[BookingIsNotConfirmed.type]
      .addConcreteType[BookingIsAlreadyCanceled.type]
      .addConcreteType[BookingIsAlreadyConfirmed.type]
      .addConcreteType[BookingIsAlreadySettled.type]
      .addConcreteType[BookingIsDenied.type]

  implicit val rejectionCodec: Codec[BookingCommandRejection] =
    aecor.macros.boopickle.BoopickleCodec.codec[BookingCommandRejection]

}
