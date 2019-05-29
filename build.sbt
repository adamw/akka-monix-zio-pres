lazy val commonSettings = commonSmlBuildSettings ++ Seq(
  organization := "com.softwaremill",
  scalaVersion := "2.12.8"
)

lazy val akkaVersion = "2.5.23"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.7" % "test"

lazy val rootProject = (project in file("."))
  .settings(commonSettings: _*)
  .settings(publishArtifact := false, name := "akka-monix-zio-pres")
  .aggregate(core)

lazy val core: Project = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalaz" %% "scalaz-zio" % "1.0-RC4",
      "io.monix" %% "monix" % "3.0.0-RC2",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      scalaTest
    )
  )

