package com.rmichau.haf

import cats.effect.{IO, Ref}
import com.rmichau.haf.clients.{CurlClient, DataSource, HadoopApp, HttpClient, QueryResult, ResourceManagerClient, JobHistoryClient, SparkHistoryClient}
import munit.CatsEffectSuite

import java.net.URI

class ApplicationIntegrationTest extends CatsEffectSuite {

  case class MockHadoopApp(
    id: String,
    user: String,
    name: String,
    dataSource: DataSource,
    trackingUrl: String,
    applicationType: String,
    startDate: String
  ) extends HadoopApp

  class MockHttpClient(responses: Map[String, String]) extends HttpClient {
    override def run(url: URI): IO[QueryResult] = {
      responses.get(url.toString) match {
        case Some(response) => IO.pure(QueryResult(response, ""))
        case None => IO.raiseError(new RuntimeException(s"No mock response for URL: $url"))
      }
    }
  }

  test("ResourceManagerClient should parse apps correctly with mocked HttpClient") {
    val mockResponse = """{
      "apps": {
        "app": [
          {
            "id": "application_1234567890123_0001",
            "user": "testuser",
            "name": "Test Application",
            "applicationType": "SPARK",
            "startedTime": 1640995200000
          }
        ]
      }
    }"""

    val mockClient = new MockHttpClient(Map(
      "http://localhost:8088/ws/v1/cluster/apps?deSelects=resourceRequests&deSelects=timeouts&deSelects=appNodeLabelExpression&deSelects=amNodeLabelExpression&deSelects=resourceInfo" -> mockResponse
    ))

    val rmClient = new ResourceManagerClient("localhost:8088", mockClient)

    rmClient.query().map { apps =>
      assertEquals(apps.length, 1)
      val app = apps.head
      assertEquals(app.id, "application_1234567890123_0001")
      assertEquals(app.user, "testuser")
      assertEquals(app.name, "Test Application")
      assertEquals(app.dataSource, DataSource.YarnResourceManager)
      assertEquals(app.applicationType, "SPARK")
    }
  }

  test("JobHistoryClient should parse jobs correctly with mocked HttpClient") {
    val mockResponse = """{
      "jobs": {
        "job": [
          {
            "id": "job_1234567890123_0001",
            "user": "testuser",
            "name": "Test Job",
            "startTime": 1640995200000
          }
        ]
      }
    }"""

    val mockClient = new MockHttpClient(Map(
      "http://localhost:19888/ws/v1/history/mapreduce/jobs" -> mockResponse
    ))

    val jhClient = new JobHistoryClient("localhost:19888", mockClient)

    jhClient.query().map { jobs =>
      assertEquals(jobs.length, 1)
      val job = jobs.head
      assertEquals(job.id, "job_1234567890123_0001")
      assertEquals(job.user, "testuser")
      assertEquals(job.name, "Test Job")
      assertEquals(job.dataSource, DataSource.YarnJobHistory)
    }
  }

  test("SparkHistoryClient should parse applications correctly with mocked HttpClient") {
    val mockResponse = """[
      {
        "id": "app-20220101123456-0001",
        "name": "Test Spark App",
        "attempts": [
          {
            "sparkUser": "testuser",
            "startTimeEpoch": 1640995200000
          }
        ]
      }
    ]"""

    val mockClient = new MockHttpClient(Map(
      "http://localhost:18080/api/v1/applications" -> mockResponse
    ))

    val sparkClient = new SparkHistoryClient("localhost:18080", mockClient)

    sparkClient.query().map { apps =>
      assertEquals(apps.length, 1)
      val app = apps.head
      assertEquals(app.id, "app-20220101123456-0001")
      assertEquals(app.user, "testuser")
      assertEquals(app.name, "Test Spark App")
      assertEquals(app.dataSource, DataSource.SparkHistory)
    }
  }

  test("MockHttpClient should handle missing URLs") {
    val mockClient = new MockHttpClient(Map.empty)
    
    mockClient.run(URI.create("http://nonexistent.com")).attempt.map { result =>
      assert(result.isLeft)
      assert(result.left.toOption.get.getMessage.contains("No mock response for URL"))
    }
  }

  test("Application integration test with all clients") {
    val rmResponse = """{
      "apps": {
        "app": [
          {
            "id": "application_1234567890123_0001",
            "user": "rmuser",
            "name": "RM Application",
            "applicationType": "SPARK",
            "startedTime": 1640995200000
          }
        ]
      }
    }"""

    val jhResponse = """{
      "jobs": {
        "job": [
          {
            "id": "job_1234567890123_0001",
            "user": "jhuser",
            "name": "JH Job",
            "startTime": 1640995200000
          }
        ]
      }
    }"""

    val sparkResponse = """[
      {
        "id": "app-20220101123456-0001",
        "name": "Spark Application",
        "attempts": [
          {
            "sparkUser": "sparkuser",
            "startTimeEpoch": 1640995200000
          }
        ]
      }
    ]"""

    val mockClient = new MockHttpClient(Map(
      "http://rm:8088/ws/v1/cluster/apps?deSelects=resourceRequests&deSelects=timeouts&deSelects=appNodeLabelExpression&deSelects=amNodeLabelExpression&deSelects=resourceInfo" -> rmResponse,
      "http://jh:19888/ws/v1/history/mapreduce/jobs" -> jhResponse,
      "http://spark:18080/api/v1/applications" -> sparkResponse
    ))

    val rmClient = new ResourceManagerClient("rm:8088", mockClient)
    val jhClient = new JobHistoryClient("jh:19888", mockClient)
    val sparkClient = new SparkHistoryClient("spark:18080", mockClient)

    for {
      rmApps <- rmClient.query()
      jhJobs <- jhClient.query()
      sparkApps <- sparkClient.query()
    } yield {
      assertEquals(rmApps.length, 1)
      assertEquals(jhJobs.length, 1)
      assertEquals(sparkApps.length, 1)

      assertEquals(rmApps.head.dataSource, DataSource.YarnResourceManager)
      assertEquals(jhJobs.head.dataSource, DataSource.YarnJobHistory)
      assertEquals(sparkApps.head.dataSource, DataSource.SparkHistory)

      assertEquals(rmApps.head.user, "rmuser")
      assertEquals(jhJobs.head.user, "jhuser")
      assertEquals(sparkApps.head.user, "sparkuser")
    }
  }
}