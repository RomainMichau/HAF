package com.rmichau.haf

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.headers.`Content-Type`
import com.comcast.ip4s.*
import cats.effect.kernel.Resource
import scala.concurrent.duration.*
import scala.util.Random
import upickle.default.*

object MockMain extends IOApp {

  // Random data generators
  val random = new Random()
  
  val userNames = Array("hadoop", "spark", "analytics", "data_engineer", "ml_engineer", "batch_user", "etl_user", "admin", "developer", "scientist")
  val appTypes = Array("SPARK", "MAPREDUCE", "TEZ", "YARN")
  val jobPrefixes = Array("ETL", "Analytics", "ML", "Data Processing", "Batch", "Stream", "spark", "Query", "Report", "Training")
  val jobSuffixes = Array("Job", "Pipeline", "Task", "Process", "Workflow", "Analysis", "Model", "Application")
  
  def generateRMApps(count: Int): String = {
    val apps = (1 to count).map { i =>
      val timestamp = System.currentTimeMillis() - random.nextInt(86400000) * 30 // Random time in last 30 days
      val appType = appTypes(random.nextInt(appTypes.length))
      val prefix = jobPrefixes(random.nextInt(jobPrefixes.length))
      val suffix = jobSuffixes(random.nextInt(jobSuffixes.length))
      s"""{
        "id": "application_${timestamp}_${i.toString.padTo(4, '0')}",
        "user": "${userNames(random.nextInt(userNames.length))}",
        "name": "$prefix $suffix ${if (random.nextBoolean()) "spark" else ""}",
        "applicationType": "$appType",
        "startedTime": $timestamp
      }"""
    }
    s"""{"apps": {"app": [${apps.mkString(",")}]}}"""
  }
  
  def generateJHJobs(count: Int): String = {
    val jobs = (1 to count).map { i =>
      val timestamp = System.currentTimeMillis() - random.nextInt(86400000) * 30
      val prefix = jobPrefixes(random.nextInt(jobPrefixes.length))
      val suffix = jobSuffixes(random.nextInt(jobSuffixes.length))
      s"""{
        "id": "job_${timestamp}_${i.toString.padTo(4, '0')}",
        "user": "${userNames(random.nextInt(userNames.length))}",
        "name": "$prefix $suffix ${if (random.nextBoolean()) "spark" else ""}",
        "startTime": $timestamp
      }"""
    }
    s"""{"jobs": {"job": [${jobs.mkString(",")}]}}"""
  }
  
  def generateSparkApps(count: Int): String = {
    val apps = (1 to count).map { i =>
      val timestamp = System.currentTimeMillis() - random.nextInt(86400000) * 30
      val prefix = jobPrefixes(random.nextInt(jobPrefixes.length))
      val suffix = jobSuffixes(random.nextInt(jobSuffixes.length))
      val dateStr = f"${2022 + random.nextInt(2)}%04d${random.nextInt(12) + 1}%02d${random.nextInt(28) + 1}%02d${random.nextInt(24)}%02d${random.nextInt(60)}%02d${random.nextInt(60)}%02d"
      s"""{
        "id": "app-$dateStr-${i.toString.padTo(4, '0')}",
        "name": "$prefix $suffix ${if (random.nextBoolean()) "spark" else ""}",
        "attempts": [{
          "sparkUser": "${userNames(random.nextInt(userNames.length))}",
          "startTimeEpoch": $timestamp
        }]
      }"""
    }
    s"""[${apps.mkString(",")}]"""
  }

  // Mock ResourceManager service (2 second delay)
  val rmService: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root / "ws" / "v1" / "cluster" / "apps" :? _ =>
      for {
        _ <- IO.sleep(2.seconds)
        response <- IO(generateRMApps(1000))
        result <- Ok(response).map(_.withContentType(`Content-Type`(MediaType.application.json)))
      } yield result
  }

  // Mock JobHistory service (5 second delay)
  val jhService: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root / "ws" / "v1" / "history" / "mapreduce" / "jobs" =>
      for {
        _ <- IO.sleep(5.seconds)
        response <- IO(generateJHJobs(1000))
        result <- Ok(response).map(_.withContentType(`Content-Type`(MediaType.application.json)))
      } yield result
  }

  // Mock Spark History service (10 second delay)
  val sparkService: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root / "api" / "v1" / "applications" =>
      for {
        _ <- IO.sleep(10.seconds)
        response <- IO(generateSparkApps(1000))
        result <- Ok(response).map(_.withContentType(`Content-Type`(MediaType.application.json)))
      } yield result
  }

  // Create servers
  def createServer(service: HttpRoutes[IO], port: Int): Resource[IO, Server] =
    EmberServerBuilder.default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(Port.fromInt(port).get)
      .withHttpApp(service.orNotFound)
      .build

  def startServers(): Resource[IO, Unit] = for {
    _ <- createServer(rmService, 8088)
    _ <- createServer(jhService, 19888)  
    _ <- createServer(sparkService, 18080)
    _ <- Resource.eval(IO.println("Mock servers started:"))
    _ <- Resource.eval(IO.println("  ResourceManager: http://localhost:8088"))
    _ <- Resource.eval(IO.println("  JobHistory: http://localhost:19888"))
    _ <- Resource.eval(IO.println("  SparkHistory: http://localhost:18080"))
    _ <- Resource.eval(IO.println(""))
  } yield ()

  def runRealApplication(): IO[ExitCode] = {
    val args = List("a", "--rmHost", "localhost:8088", "--jhHost", "localhost:19888", "--shHost", "localhost:18080")
    Application.run(args)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    startServers().use { _ =>
      for {
        _ <- IO.println("Starting real Application against mock servers...")
        _ <- IO.sleep(1.second) // Give servers time to start
        exitCode <- runRealApplication()
      } yield exitCode
    }
  }
}