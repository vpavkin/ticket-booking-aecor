package ru.pavkin.booking.common.json

import io.circe.{Decoder, Encoder}
import shapeless.Unwrapped

trait AnyValCoders {
  implicit def anyValEncoder[V, U](
    implicit ev: V <:< AnyVal,
    V: Unwrapped.Aux[V, U],
    encoder: Encoder[U]): Encoder[V] = {
    val _ = ev
    encoder.contramap(V.unwrap)
  }

  implicit def anyValDecoder[V, U](
    implicit ev: V <:< AnyVal,
    V: Unwrapped.Aux[V, U],
    decoder: Decoder[U],
    lp: shapeless.LowPriority): Decoder[V] = {
    val _ = (ev, lp)
    decoder.map(V.wrap)
  }
}

object AnyValCoders extends AnyValCoders
