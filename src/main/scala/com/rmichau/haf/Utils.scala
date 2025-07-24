package com.rmichau.haf

import scala.concurrent.duration._
import cats.effect.{IO, Resource}

object Utils {
  def spinner(text: String = "Loading", interval: FiniteDuration = 100.millis): Resource[IO, Unit] = {
    val frames = Seq("|", "/", "-", "\\")
    def loop(idx: Int): IO[Unit] = IO.print(s"\r$text ${frames(idx % frames.length)}") >> IO.sleep(interval) >> loop((idx + 1) % frames.length)
    Resource.make(loop(0).start)(_.cancel).map(_ => ())
  }
}
