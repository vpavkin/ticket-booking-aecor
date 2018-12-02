package ru.pavkin.booking.booking.entity

import java.time.Instant

import boopickle.Default._
import scodec.Codec

object BookingWireCodecs {

  implicit val instantPickler: boopickle.Pickler[Instant] =
    boopickle.DefaultBasic.longPickler.xmap(Instant.ofEpochMilli)(_.toEpochMilli)

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
