package com.rmichau.haf.clients

import cats.effect.IO
import upickle.default.*

import java.net.URI
import scala.util.Try

class JobHistoryClient(host: String, httpClient: HttpClient = CurlClient) extends Client {
  def query(): IO[Seq[HadoopApp]] = {
    for {
      resSt <- httpClient.run(URI.create(s"http://$host/ws/v1/history/mapreduce/jobs"))
    } yield Try{read[Root](resSt.stdout).jobs.job} match {
      case scala.util.Success(jobs) => jobs
      case scala.util.Failure(ex) => throw new RuntimeException(s"Failed to parse job history: ${ex.getMessage}, ${resSt.stdout}", ex)
    }
  }

  case class Job(
                  id: String,
                  user: String,
                  name: String,
                  startTime: Long
                )
    extends HadoopApp {
    val dataSource = DataSource.YarnJobHistory
    val trackingUrl = s"http://$host/jobhistory/job/$id"

    override val applicationType: String = "???"

    override def startDate: String = {
      val date = new java.util.Date(startTime)
      val formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      formatter.format(date)
    }
  }

  case class Jobs(job: Seq[Job])

  case class Root(jobs: Jobs)

  given ReadWriter[Root] = macroRW

  given ReadWriter[Jobs] = macroRW

  given ReadWriter[Job] = macroRW
}
