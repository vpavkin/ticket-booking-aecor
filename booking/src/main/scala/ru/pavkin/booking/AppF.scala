package ru.pavkin.booking

import akka.actor.ActorSystem
import cats.effect._
import cats.implicits._
import cats.temp.par.Par
import com.typesafe.config.ConfigFactory
import pureconfig.generic.auto._
import pureconfig.loadConfigOrThrow
import ru.pavkin.booking.config.AppConfig
import ru.pavkin.booking.common.syntax._

class AppF[F[_]: Timer: ContextShift: Par: LiftIO](implicit F: ConcurrentEffect[F]) {

  case class Resources(appConfig: AppConfig,
                       system: ActorSystem,
                       postgresWirings: PostgresWirings[F])

  def resources: Resource[F, Resources] =
    for {
      config <- F.delay(ConfigFactory.load()).resource
      appConfig <- F.delay(loadConfigOrThrow[AppConfig](config)).resource
      system <- Resource.make(F.delay(ActorSystem(appConfig.cluster.systemName, config)))(
                 s => LiftIO[F].liftIO(IO.fromFuture(IO(s.terminate()))).void
               )
      postgresWirings <- PostgresWirings[F](appConfig)
      _ <- Resource.make(F.unit)(_ => F.delay(println("Releasing application resources")))
    } yield Resources(appConfig, system, postgresWirings)

  def launch(r: Resources): F[Unit] = {
    import r._

    for {
      entityWirings <- EntityWirings[F](system, postgresWirings)
      serviceWirings <- ServiceWirings[F]()
      processWirings = new ProcessWirings[F](system, postgresWirings, serviceWirings, entityWirings)
      endpointWirings = new EndpointWirings[F](appConfig.httpServer, postgresWirings, entityWirings)
      _ <- processWirings.launchProcesses.void
      _ <- endpointWirings.launchHttpService
    } yield ()
  }

  def run: Resource[F, Unit] = resources.flatMap(r => launch(r).resource)
}
