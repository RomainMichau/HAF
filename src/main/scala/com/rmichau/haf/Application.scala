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
  def filterAndPrint(filter: String, appsOutcome: Outcome[IO, Throwable, Seq[HadoopApp]]): IO[Unit] = {
    appsOutcome match {
      case Outcome.Succeeded(appsIO) => appsIO.flatMap { apps =>
        val filteredApps = apps.filter(_.toString.contains(filter))
        IO(println(s"Filtered Apps: ${filteredApps.map(_.prettyPrint(filter)).mkString("\n")}"))
      }
    }
  }

  override def main: Opts[IO[ExitCode]] = {
    HafCli.hafConfig.map { config =>
      val rmClient = new ResourceManagerClient(config.rmHost)
      val jhClient = new JobHistoryClient(config.jhHost)
      val sparkClient = new SparkHistoryClient(config.shHost)
      for {
        _ <- IO(println(s"Filtering applications with: ${config.filter}"))
        sparkAppsIO <- sparkClient.query().start
        rmAppsIO <- rmClient.query().start
        jhAppIO <- jhClient.query().start
        rmAppsOutcome <- Utils.spinner("ResourceManager Apps").use(_ => rmAppsIO.join)
        _ <- IO(println(s"=====  ResourceManager Apps  ====="))
        _ <- filterAndPrint(config.filter, rmAppsOutcome)
        _ <- IO(println(s"=====  JobHistory Apps  ====="))
        jhAppsOutcome <- Utils.spinner("JobHistory Apps").use(_ => jhAppIO.join)
        _ <- filterAndPrint(config.filter, jhAppsOutcome)
        _ <- IO(println(s"=====  SparkHistory Apps  ====="))
        sparkHistoryOutcome <- Utils.spinner("SparkHistory Apps").use(_ => sparkAppsIO.join)
        _ <- filterAndPrint(config.filter, sparkHistoryOutcome)
      } yield ExitCode.Success
    }
  }
}