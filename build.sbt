import sbt.Keys.{parallelExecution, scalacOptions}
import sbt._
import sbt.Keys._

lazy val aecorVersion = "0.18.0"
lazy val aecorPostgresVersion = "0.3.0"
lazy val akkaVersion = "2.5.18"
lazy val boopickleVerison = "1.3.0"
lazy val catsMTLVersion = "0.4.0"
lazy val catsVersion = "1.4.0"
lazy val log4CatsVersion = "0.2.0"
lazy val circeDerivationVersion = "0.10.0-M1"
lazy val circeVersion = "0.10.1"
lazy val doobieVersion = "0.6.0"
lazy val logbackVersion = "1.2.3"
lazy val metaParadiseVersion = "3.0.0-M11"
lazy val pureConfigVersion = "0.10.0"
lazy val scalaCheckVersion = "1.14.0"
lazy val scalaTestVersion = "3.0.5"
lazy val shapelessVersion = "2.3.3"
lazy val http4sVersion = "0.20.0-M3"
lazy val enumeratumVersion = "1.5.13"

lazy val simulacrum = "com.github.mpilquist" %% "simulacrum" % "0.12.0"
lazy val kindProjector = compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")
lazy val betterMonadicFor = addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")
lazy val scalapbRuntime = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"

lazy val booking = (project in file("booking"))
  .settings(
    baseSettings,
    betterMonadicFor,
    fork := true,
    libraryDependencies ++= Seq(
      kindProjector,
      compilerPlugin("org.scalameta" % "paradise" % metaParadiseVersion cross CrossVersion.full),
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      scalapbRuntime,
      "com.beachape" %% "enumeratum" % enumeratumVersion,
      "com.beachape" %% "enumeratum-circe" % enumeratumVersion,
      "io.aecor" %% "core" % aecorVersion,
      "io.aecor" %% "schedule" % aecorVersion,
      "io.aecor" %% "akka-cluster-runtime" % aecorVersion,
      "io.aecor" %% "distributed-processing" % aecorVersion,
      "io.aecor" %% "boopickle-wire-protocol" % aecorVersion,
      "io.aecor" %% "aecor-postgres-journal" % aecorPostgresVersion,
      "io.aecor" %% "test-kit" % aecorVersion % Test,
      "io.chrisdavenport" %% "log4cats-core" % log4CatsVersion,
      "io.chrisdavenport" %% "log4cats-slf4j" % log4CatsVersion,
      "io.chrisdavenport" %% "cats-par" % "0.2.0",
      "io.monix" %% "monix" % "3.0.0-RC2",
      "com.ovoenergy" %% "fs2-kafka" % "0.16.4",
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "org.tpolecat" %% "doobie-hikari" % doobieVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-derivation" % circeDerivationVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-java8" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.suzaku" %% "boopickle-shapeless" % boopickleVerison,
      "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-http4s" % pureConfigVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "org.scalacheck" %% "scalacheck" % scalaCheckVersion % Test
    ),
    scalacOptions += "-Xplugin-require:macroparadise",
    sources in(Compile, doc) := Nil, // macroparadise doesn't work with scaladoc yet.
    mainClass in Compile := Some("ru.pavkin.booking.App"),
    PB.targets in Compile := Seq(
      scalapb.gen(flatPackage = true) -> (sourceManaged in Compile).value
    ),
  )
  .enablePlugins(DockerPlugin, JavaAppPackaging)

lazy val baseSettings = Seq(
  scalaVersion in ThisBuild := "2.12.7",
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    Resolver.bintrayRepo("ovotech", "maven")
  ),
  scalacOptions in(Compile, console) ~= {
    _.filterNot(unusedWarnings.toSet + "-Ywarn-value-discard")
  },
  scalacOptions ++= commonScalacOptions,
  scalacOptions ++= Seq("-Xmax-classfile-name", "128"),
  parallelExecution in Test := false,
  sources in(Compile, doc) := Nil,
  dockerExposedPorts := Seq(9000),
  dockerBaseImage := "java:8.161",
  publishTo := None,
  cancelable in Global := true
)

lazy val commonScalacOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Ypartial-unification"
) ++ unusedWarnings

//lazy val unusedWarnings = Seq.empty[String]
lazy val unusedWarnings = Seq("-Ywarn-unused", "-Ywarn-unused-import")
