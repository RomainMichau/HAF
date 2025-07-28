package com.rmichau.haf

import scala.concurrent.duration.*
import cats.effect.{IO, Ref, Resource}

class Spinner(spinnersRef: Ref[IO, Set[String]]) {
  private val frames = List("⠋","⠙","⠹","⠸","⠼","⠴","⠦","⠧","⠇","⠏")
  private def loop(idx: Int): IO[Unit] = for {
    spinners <- spinnersRef.get
    _ <- IO.print(s"\rLoading [${spinners.mkString(" , ")}] ${frames(idx % frames.length)}") >> IO.sleep(200.millis) >> loop((idx + 1) % frames.length)
  } yield ()
    
  
  def removeSpinner(spinner: String): IO[Unit] = spinnersRef.update(_ - spinner)
  def addSpinner(spinner: String): IO[Unit] = spinnersRef.update(_ + spinner)
  def letsSpin: IO[Unit] = loop(0)
}
object Utils {
  
  def initSpinner(waitingFor: Set[String] = Set.empty): IO[Spinner] = for {
    ref <- Ref[IO].of(waitingFor)
  } yield Spinner(ref)

  def printArt(): IO[Unit] = IO(println(
    """
      | ___  ___  ________  ________
      ||\  \|\  \|\   __  \|\  _____\
      |\ \  \\\  \ \  \|\  \ \  \__/
      | \ \   __  \ \   __  \ \   __\
      |  \ \  \ \  \ \  \ \  \ \  \_|
      |   \ \__\ \__\ \__\ \__\ \__\
      |    \|__|\|__|\|__|\|__|\|__|
    """.stripMargin))
}
