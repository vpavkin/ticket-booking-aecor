package ru.pavkin.common

import aecor.data.EventsourcedBehavior
import cats.effect.{Concurrent, Timer}
import cats.tagless.FunctorK

import scala.concurrent.duration.FiniteDuration

object TimedOutBehaviour {

  def apply[M[_[_]], F[_], S, E](behaviour: EventsourcedBehavior[M, F, S, E])(
    timeout: FiniteDuration
  )(implicit timer: Timer[F], F: Concurrent[F], M: FunctorK[M]): EventsourcedBehavior[M, F, S, E] =
    behaviour.mapK(TimedOut[F](timeout))

}
