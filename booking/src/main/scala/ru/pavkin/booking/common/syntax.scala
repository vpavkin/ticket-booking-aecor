package ru.pavkin.booking.common

import cats.Applicative
import cats.effect.Resource

object syntax {

  implicit class ResourceListOption[F[_], A](val fa: F[A]) extends AnyVal {
    def resource(implicit F: Applicative[F]): Resource[F, A] = Resource.liftF(fa)
  }
}
