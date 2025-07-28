package com.rmichau.haf

import cats.effect.{ExitCode, IO, Outcome}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import com.rmichau.haf.clients.{HadoopApp, JobHistoryClient, ResourceManagerClient, SparkHistoryClient}
import com.rmichau.haf.Utils

object Application extends CommandIOApp(
  name = "haf",
  header = "Hadoop Application Finder"
) {
  // Print the HAF ASCII art
  private def printArt(): IO[Unit] = IO(println(
    """
      | ___  ___  ________  ________
      ||\  \|\  \|\   __  \|\  _____\
      |\ \  \\\  \ \  \|\  \ \  \__/
      | \ \   __  \ \   __  \ \   __\
      |  \ \  \ \  \ \  \ \  \ \  \_|
      |   \ \__\ \__\ \__\ \__\ \__\
      |    \|__|\|__|\|__|\|__|\|__|
    """.stripMargin))

  // Print filter info
  private def printFilter(filter: String): IO[Unit] =
    IO(println(s"Filtering applications with: $filter"))

  // Query and print results for a given client
  private def queryAndPrint(
    label: String,
    queryIO: IO[cats.effect.Fiber[IO, Throwable, Seq[HadoopApp]]],
    filter: String
  ): IO[Unit] = for {
    fiber <- queryIO
    outcome <- Utils.spinner(label).use(_ => fiber.join)
    _ <- outcome match {
      case Outcome.Succeeded(appsIO) =>
        appsIO.flatMap { apps =>
          val filteredApps = apps.filter(_.toString.contains(filter))
          IO(println(s"Filtered Apps: ${filteredApps.map(_.prettyPrint(filter)).mkString("\n")}"))
        }
      case _ => IO.unit
    }
  } yield ()

  override def main: Opts[IO[ExitCode]] = {
    HafCli.hafConfig.map { config =>
      val rmClient = new ResourceManagerClient(config.rmHost)
      val jhClient = new JobHistoryClient(config.jhHost)
      val sparkClient = new SparkHistoryClient(config.shHost)
      for {
        _ <- printArt()
        _ <- printFilter(config.filter)
        _ <- if (config.useRM) queryAndPrint("ResourceManager Apps", rmClient.query().start, config.filter) else IO.unit
        _ <- if (config.useJH) queryAndPrint("JobHistory Apps", jhClient.query().start, config.filter) else IO.unit
        _ <- if (config.useSH) queryAndPrint("SparkHistory Apps", sparkClient.query().start, config.filter) else IO.unit
      } yield ExitCode.Success
    }
  }
}