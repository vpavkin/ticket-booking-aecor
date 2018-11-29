package ru.pavkin.booking.config

import aecor.journal.postgres.PostgresEventJournal

final case class AppConfig(
  httpServer: HttpServer,
  cluster: ActorSystemName,
  postgres: PostgresConfig,
  postgresJournals: PostgresJournals)

final case class PostgresConfig(
  contactPoints: String,
  port: Int,
  database: String,
  username: String,
  password: String)

final case class HttpServer(interface: String, port: Int)

final case class ActorSystemName(systemName: String)

final case class PostgresJournals(booking: PostgresEventJournal.Settings)
