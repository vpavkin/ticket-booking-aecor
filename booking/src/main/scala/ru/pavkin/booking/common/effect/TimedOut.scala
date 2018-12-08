package ru.pavkin.booking.common.effect
import cats.effect.{Concurrent, Timer}
import cats.~>

import scala.concurrent.TimeoutException
import scala.concurrent.duration.FiniteDuration

object TimedOut {
  def apply[F[_]](timeout: FiniteDuration)(implicit timer: Timer[F], F: Concurrent[F]): F ~> F =
    new (F ~> F) {
      def apply[A](fa: F[A]): F[A] =
        Concurrent.timeoutTo(
          fa,
          timeout,
          F.raiseError(new TimeoutException(s"Call timed out after $timeout"))
        )
    }
}
