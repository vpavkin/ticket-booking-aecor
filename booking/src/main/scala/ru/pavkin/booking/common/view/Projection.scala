package ru.pavkin.booking.common.view

import aecor.data._
import cats.tagless.autoFunctorK
import ru.pavkin.booking.common.view.Projection.Version

@autoFunctorK(false)
trait Projection[F[_], E, S] {
  def fetchVersionAndState(event: E): F[(Version, Option[S])]
  def saveNewVersion(s: S, version: Version): F[Unit]
  def applyEvent(s: Option[S])(event: E): Folded[Option[S]]
}

object Projection {

  final case class Version(value: Long) extends AnyVal {
    def next: Version = Version(value + 1)
  }
  val initialVersion = Version(0)

  def contramapEvents[F[_], E1, E2, S](p: Projection[F, E1, S])(f: E2 => E1): Projection[F, E2, S] =
    new Projection[F, E2, S] {
      def fetchVersionAndState(event: E2): F[(Version, Option[S])] =
        p.fetchVersionAndState(f(event))
      def saveNewVersion(s: S, version: Version): F[Unit] =
        p.saveNewVersion(s, version)
      def applyEvent(s: Option[S])(event: E2): Folded[Option[S]] =
        p.applyEvent(s)(f(event))
    }

  def ignoreIdentity[F[_], K, E, S](p: Projection[F, E, S]): Projection[F, EntityEvent[K, E], S] =
    contramapEvents(p)(_.payload)

}
