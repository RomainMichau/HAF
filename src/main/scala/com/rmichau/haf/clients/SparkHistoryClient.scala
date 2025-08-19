package com.rmichau.haf.clients

import cats.effect.IO
import upickle.default.*

import java.net.URI
import scala.util.Try

class SparkHistoryClient(host: String, httpClient: HttpClient = CurlClient) extends Client {
  def query(): IO[Seq[HadoopApp]] = {
    val url = URI.create(s"http://$host/api/v1/applications")
    for {
      resSt <- httpClient.run(url)
    } yield Try(read[Seq[Job]](resSt.stdout)) match {
      case scala.util.Success(jobs) => jobs
      case scala.util.Failure(ex) => throw new RuntimeException(s"Failed to parse Spark History response: ${ex.getMessage}, ${resSt.stdout}", ex)
    }
  }

  case class Job(
                  id: String,
                  name: String,
                  attempts: Seq[Attempt],
                ) extends HadoopApp {
    val trackingUrl = s"http://$host/history/$id"
    val user = attempts.headOption.map(_.sparkUser).getOrElse("unknown")
    val dataSource = DataSource.SparkHistory
    val applicationType = "???"

    override def startDate: String = {
      attempts.headOption match {
        case Some(attempt) =>
          val date = new java.util.Date(attempt.startTimeEpoch)
          val formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
          formatter.format(date)
        case None => "????"
      }
    }

    override def state: String = "NOT HANDLED BY SH"

    override def finalStatus: String = "NOT HANDLED BY SH"
  }

  case class Attempt(sparkUser: String,startTimeEpoch: Long)

  given ReadWriter[Attempt] = macroRW

  given ReadWriter[Job] = macroRW
}
