package com.rmichau.haf.clients

import cats.effect.IO

import java.net.URI
import scala.util.{Failure, Try}

case class QueryResult(stdout: String, stderr: String)
trait HttpClient {
  def run(url: URI): IO[QueryResult]
}

object CurlClient extends HttpClient{
  def run(url: URI): IO[QueryResult] =
      for {
        result <- IO.blocking(os.proc(s"/usr/bin/curl","-s", "-L" ,"--negotiate", "-u", ":", url.toString).call(stdout = os.Pipe, stderr = os.Pipe))
      } yield {
        QueryResult(result.out.text(), result.err.text())
      }
}
