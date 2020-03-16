name := "http-request-signer"

lazy val scala212 = "2.12.10"
lazy val scala211 = "2.13.1"
lazy val supportedScalaVersions = List(scala212, scala211)

ThisBuild / organization := "pl.abankowski"
ThisBuild / version      := "0.1.2"
ThisBuild / scalaVersion := scala212

ThisBuild / githubOwner := "abankowski"
ThisBuild / githubRepository := "http-request-signer"

lazy val versions = new {
  val akkaHttp = "10.1.9"
  val akkaStream = "2.6.0"
  val catsVersion = "2.0.0"
  val http4sVersion = "0.21.1"
}

lazy val http4sDependencies = Seq(
  "org.http4s" %% "http4s-dsl",
  "org.http4s" %% "http4s-blaze-client",
).map(_ % versions.http4sVersion)

lazy val akkaHttpDependencies = Seq(
  "com.typesafe.akka" %% "akka-http" % versions.akkaHttp,
  "com.typesafe.akka" %% "akka-stream" % versions.akkaStream,
)

lazy val catsDependencies = Seq(
  "org.typelevel" %% "cats-effect",
  "org.typelevel" %% "cats-core",
).map(_ % versions.catsVersion)

lazy val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.8",
  "org.scalactic" %% "scalactic" % "3.0.8",
  "org.scalamock" %% "scalamock" % "4.4.0",
).map(_ % Test)

lazy val core = (project in file("core"))
  .settings(
    name := "http-request-signer-core",
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Seq("org.bouncycastle" % "bcprov-jdk15on" % "1.64")
  )

lazy val http4s = (project in file("http4s"))
  .settings(
    name := "http4s-request-signer",
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= http4sDependencies.map(_  % "provided") ++ catsDependencies ++ testDependencies)
  .dependsOn(core)

lazy val akkaHttp = (project in file("akka-http"))
  .settings(
    name := "akka-http-request-signer",
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= akkaHttpDependencies.map(_ % "provided") ++ catsDependencies ++ testDependencies ++
                            Seq("com.typesafe.akka" %% "akka-testkit" % "2.6.0" % Test)
  )
  .dependsOn(core)

lazy val root = (project in file("."))
  .aggregate(core, akkaHttp, http4s)
  .settings(
    scalaVersion := scala212,
    crossScalaVersions := Nil,
    publish / skip := true,
    libraryDependencies ++= http4sDependencies ++ akkaHttpDependencies ++ testDependencies ++ Seq(
      "com.typesafe.akka" %% "akka-testkit" % "2.6.0" % Test)
  )
  .dependsOn(akkaHttp, http4s)
