val scala3Version = "3.7.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "haf",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test,
    libraryDependencies += "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test,
    libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.11.4",
    libraryDependencies += "com.lihaoyi" %% "upickle" % "4.2.1",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.5.2",
    libraryDependencies += "com.monovore" %% "decline-effect" % "2.4.1",
    libraryDependencies += "org.http4s" %% "http4s-ember-server" % "0.23.24",
    libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.23.24",
    // sbt-assembly settings
    mainClass := Some("com.rmichau.haf.Application"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    }
  )
