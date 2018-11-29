package ru.pavkin.booking.common.view

import aecor.journal.postgres.Offset
import cats.Order
import cats.instances.long._

object instances {
  implicit val offsetOrder: Order[Offset] = Order.by(_.value)
}
