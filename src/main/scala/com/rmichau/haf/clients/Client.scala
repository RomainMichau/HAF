package com.rmichau.haf.clients

import cats.effect.IO

enum DataSource {
  case SparkHistory
  case YarnResourceManager
  case YarnJobHistory
}
trait HadoopApp {
  def id: String
  def user: String
  def name: String
  def dataSource: DataSource
  def trackingUrl: String
  override def toString: String = {
    s"id=$id, user=$user, name=$name, trackingUrl=$trackingUrl, dataSource=$dataSource"
  }

  def prettyPrint(filter: String = ""): String = {
    val magenta = "\u001b[35m"
    val cyan = "\u001b[36m"
    val yellow = "\u001b[33m"
    val green = "\u001b[32m"
    val red = "\u001b[31m"
    val bold = "\u001b[1m"
    val reset = "\u001b[0m"
    def highlight(text: String): String =
      if (filter.nonEmpty) text.replaceAllLiterally(filter, s"${bold}${red}$filter${reset}") else text
    s"${magenta}Data Source:${reset} " + dataSource + "\n" +
    s"${cyan}ID:${reset} " + highlight(id) + "\n" +
      s"${yellow}User:${reset} " + highlight(user) + "\n" +
      s"${green}Name:${reset} " + highlight(name) + "\n" +
      s"Tracking URL: " + highlight(trackingUrl) + "\n"
  }
}
trait Client {
  def query(): IO[Seq[HadoopApp]]
}
