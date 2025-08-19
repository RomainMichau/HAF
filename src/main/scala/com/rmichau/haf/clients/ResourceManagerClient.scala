package com.rmichau.haf.clients

import cats.effect.IO
import upickle.default.*

import scala.util.Try

class ResourceManagerClient(host: String, httpClient: HttpClient = CurlClient) extends Client {
  def query(): IO[Seq[HadoopApp]] = {
    val url = s"http://$host/ws/v1/cluster/apps?deSelects=resourceRequests&deSelects=timeouts&deSelects=appNodeLabelExpression&deSelects=amNodeLabelExpression&deSelects=resourceInfo"
    for {
      resSt <- httpClient.run(java.net.URI.create(url))
    } yield Try(read[Root](resSt.stdout).apps.app) match {
      case scala.util.Success(apps) => apps
      case scala.util.Failure(ex) => throw new RuntimeException(s"Failed to parse ResourceManager response: ${ex.getMessage}, ${resSt.stdout}", ex)
    }
  }


  case class RMApp(
                    id: String,
                    user: String,
                    name: String,
                    maybeTrackingUrl: Option[String] = None,
                    applicationType: String,
                    startedTime: Long,
                    finalStatus: String,
                    state: String
                  ) extends HadoopApp {
    override val trackingUrl: String = maybeTrackingUrl.getOrElse(s"http://$host/cluster/app/$id")
    val dataSource: DataSource = DataSource.YarnResourceManager

    override val startDate: String = {
      val date = new java.util.Date(startedTime)
      val formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      formatter.format(date)
    }
  }

  case class Apps(app: Seq[RMApp])

  case class Root(apps: Apps)

  given ReadWriter[Root] = macroRW

  given ReadWriter[Apps] = macroRW

  given ReadWriter[RMApp] = macroRW
}
