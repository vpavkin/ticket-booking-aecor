package ru.pavkin.booking.booking

import aecor.runtime.Eventsourced.Entities
import ru.pavkin.booking.common.models.BookingKey
import ru.pavkin.booking.booking.entity.{Booking, BookingCommandRejection}

package object booking {
  type Bookings[F[_]] = Entities.Rejectable[BookingKey, Booking, F, BookingCommandRejection]
}
