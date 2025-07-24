package com.rmichau.haf.clients

import cats.effect.IO
import upickle.default.*

import scala.util.Try

class ResourceManagerClient(host: String) extends Client {
  def query(): IO[Seq[HadoopApp]] = {
    val url = s"http://$host/ws/v1/cluster/apps?deSelects=resourceRequests&deSelects=timeouts&deSelects=appNodeLabelExpression&deSelects=amNodeLabelExpression&deSelects=resourceInfo"
    for {
      resSt <-  ClientHelper.run(java.net.URI.create(url))
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
                  ) extends HadoopApp {
    override val trackingUrl: String = maybeTrackingUrl.getOrElse(s"http://$host/cluster/app/$id")
    val dataSource: DataSource = DataSource.YarnResourceManager
  }

  case class Apps(app: Seq[RMApp])

  case class Root(apps: Apps)

  given ReadWriter[Root] = macroRW

  given ReadWriter[Apps] = macroRW

  given ReadWriter[RMApp] = macroRW
}
