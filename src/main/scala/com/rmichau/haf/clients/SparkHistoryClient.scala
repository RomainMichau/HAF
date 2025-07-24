package com.rmichau.haf.clients

import cats.effect.IO
import upickle.default.*

import java.net.URI
import scala.util.Try

class SparkHistoryClient(host: String) extends Client {
  def query(): IO[Seq[HadoopApp]] = {
    val url = URI.create(s"http://$host/api/v1/applications")
    for {
      resSt <- ClientHelper.run(url)
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
    val trackingUrl = s"http://$host/jobhistory/job/$id"
    val user = attempts.headOption.map(_.sparkUser).getOrElse("unknown")
    val dataSource = DataSource.SparkHistory
  }

  case class Attempt(sparkUser: String)

  given ReadWriter[Attempt] = macroRW

  given ReadWriter[Job] = macroRW
}
