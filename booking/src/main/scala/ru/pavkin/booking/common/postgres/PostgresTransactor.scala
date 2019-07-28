package ru.pavkin.booking.common.postgres

import cats.effect.{Async, ContextShift, Resource}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import ru.pavkin.booking.config.PostgresConfig

object PostgresTransactor {
  def transactor[F[_]](
    config: PostgresConfig
  )(implicit F: Async[F], C: ContextShift[F]): Resource[F, HikariTransactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      te <- ExecutionContexts.cachedThreadPool[F]
      tr <- HikariTransactor.newHikariTransactor[F](
             "org.postgresql.Driver",
             s"jdbc:postgresql://${config.contactPoints}:${config.port}/${config.database}",
             config.username,
             config.password,
             ce,
             te
           )
      _ <- Resource.liftF(tr.configure(ds => F.delay(ds.setAutoCommit(false))))
    } yield tr
}
