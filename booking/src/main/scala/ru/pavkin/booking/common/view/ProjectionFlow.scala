package ru.pavkin.booking.common.view

import aecor.data.{Committable, EntityEvent}
import cats.effect.Sync
import io.chrisdavenport.log4cats.Logger
import cats.implicits._

/**
  * Consumes a stream of events by folding them into view
  */
object ProjectionFlow {

  def apply[F[_], K, E, S](log: Logger[F],
                           aggregateProjection: Projection[F, EntityEvent[K, E], S],
  )(implicit F: Sync[F]): fs2.Pipe[F, Committable[F, EntityEvent[K, E]], Unit] = {

    def foldEvent(event: EntityEvent[K, E], state: Option[S]): F[Option[S]] = {
      val newVersion = aggregateProjection.applyEvent(state)(event)
      log.debug(s"New version [$newVersion]") >>
        newVersion
          .fold(
            F.raiseError[Option[S]](
              new IllegalStateException(s"Projection failed for state = [$state], event = [$event]")
            )
          )(_.pure[F])
    }

    def runProjection(event: EntityEvent[K, E]): F[Unit] =
      for {
        (currentVersion, currentState) <- aggregateProjection.fetchVersionAndState(event)
        _ <- log.debug(s"Current $currentVersion [$currentState]")
        _ <- F.whenA(currentVersion.value < event.sequenceNr) {
              foldEvent(event, currentState).flatMap {
                case None => F.unit

                case Some(state) =>
                  aggregateProjection.saveNewVersion(state, currentVersion.next)
              }
            }
      } yield ()

    _.evalMap(_.traverse(runProjection)).evalMap(_.commit)
  }
}
