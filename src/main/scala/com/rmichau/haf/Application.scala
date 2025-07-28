package com.rmichau.haf

import cats.data.OptionT
import cats.effect.{ExitCode, Fiber, FiberIO, IO, Outcome}
import cats.syntax.parallel._
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import com.rmichau.haf.clients.{Client, HadoopApp, JobHistoryClient, ResourceManagerClient, SparkHistoryClient}
import com.rmichau.haf.Utils

object Application extends CommandIOApp(
  name = "haf",
  header = "Hadoop Application Finder"
) {
  private def printFilter(filter: String): IO[Unit] =
    IO(println(s"Filtering applications with: $filter"))

  // Query and print results for a given client
  def requestClientIfDefined(
                              label: String,
                              hostOpt: Option[String],
                              createClient: String => Client,
                              filter: String,
                              spinner: Spinner
                            ): IO[Fiber[IO, Throwable, Unit]] = {
    hostOpt
      .map { host =>
        createClient(host).query()
          .map(_.filter(_.toString.contains(filter)))
          .flatTap(_ => spinner.removeSpinner(label))
          .flatMap(filteredApps => IO(println(s"Filtered Apps: ${filteredApps.map(_.prettyPrint(filter)).mkString("\n")}")))
          .start
      }
      .getOrElse(IO.unit.start)
  }

  override def main: Opts[IO[ExitCode]] = {
    HafCli.hafConfig.map { config =>
      val filter = config.filter
      for {
        _ <- Utils.printArt()
        _ <- printFilter(config.filter)
        spinner <- Utils.initSpinner()
        spinFiber <- spinner.letsSpin.start
        rmFiber <- requestClientIfDefined("RM Apps", config.rmHost, new ResourceManagerClient(_), config.filter, spinner)
        _ <- spinner.addSpinner("RM Apps")
        jhFiber <- requestClientIfDefined("Job History Apps", config.jhHost, new JobHistoryClient(_), config.filter, spinner)
        _ <- spinner.addSpinner("Job History Apps")
        spFiber <- requestClientIfDefined("Spark History Apps", config.shHost, new SparkHistoryClient(_), config.filter, spinner)
        _ <- spinner.addSpinner("Spark History Apps")
        _ <- (spFiber.joinWithNever, jhFiber.joinWithNever, rmFiber.joinWithNever).parTupled
      } yield ExitCode.Success
    }
  }
}