logLevel := Level.Warn

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.4")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.14")

addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.15")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.19")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.8.2"
