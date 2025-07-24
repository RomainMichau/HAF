package com.rmichau.haf.clients

import cats.effect.IO
import upickle.default.*

import java.net.URI
import scala.util.Try

class JobHistoryClient(host: String) extends Client {
  def query(): IO[Seq[HadoopApp]] = {
    for {
      resSt <- ClientHelper.run(URI.create(s"http://$host/ws/v1/history/mapreduce/jobs"))
    } yield Try{read[Root](resSt.stdout).jobs.job} match {
      case scala.util.Success(jobs) => jobs
      case scala.util.Failure(ex) => throw new RuntimeException(s"Failed to parse job history: ${ex.getMessage}, ${resSt.stdout}", ex)
    }
  }

  case class Job(
                  id: String,
                  user: String,
                  name: String,
                )
    extends HadoopApp {
    val dataSource = DataSource.YarnJobHistory
    val trackingUrl = s"http://$host/jobhistory/job/$id"
  }

  case class Jobs(job: Seq[Job])

  case class Root(jobs: Jobs)

  given ReadWriter[Root] = macroRW

  given ReadWriter[Jobs] = macroRW

  given ReadWriter[Job] = macroRW
}
