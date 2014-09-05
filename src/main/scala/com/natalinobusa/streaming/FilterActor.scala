package com.natalinobusa.streaming

import akka.actor._
import akka.util.Timeout
import com.natalinobusa.streaming.models.Messages.{postResponse, Get}
import spray.http.MediaTypes._
import spray.util._

import akka.io.IO
import spray.can.Http

import scala.collection.mutable

import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration

import spray.http._
import spray.httpx.encoding.{Gzip, Deflate}
import spray.client.pipelining._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import HttpMethods._
import HttpEntity._

object FilterActor {
  def props(id: Int, stream_id: Int, resolution: Int, transform: String, group_by: Option[String]): Props =
    Props(new FilterActor(id, stream_id, resolution, transform, group_by))
}

class FilterActor(id: Int, stream_id: Int, resolution: Int, transform: String, group_by: Option[String]) extends Actor with ActorLogging {

  def actorRefFactory = context

  implicit def executionContext = actorRefFactory.dispatcher

  var count = 0
  val acc = mutable.HashMap.empty[String, Double]
  val store = mutable.HashMap.empty[String, Double]

  def sum(value: Double, key: String) = {
    val curr = acc.getOrElseUpdate(key, 0)
    acc(key) = curr + value
  }

  def avg(value: Double, key: String) = {
    // iterative arith mean
    val curr = acc.getOrElseUpdate(key, value)
    acc(key) = curr * (count - 1) / count + value / count
  }

  def max(value: Double, key: String) = {
    val curr = acc.getOrElseUpdate(key, value)
    acc(key) = if (curr > value) curr else value
  }

  def min(value: Double, key: String) = {
    val curr = acc.getOrElseUpdate(key, value)
    acc(key) = if (curr > value) value else curr
  }

  def count(value: Double, key: String) = {
    val curr = acc.getOrElseUpdate(key, 0)
    acc(key) = curr + 1
  }

  override def preStart() = {
    in(Duration(resolution, SECONDS)) {
      self ! "tick"
    }
  }

  import com.natalinobusa.streaming.models.JsonConversions._

  private implicit val timeout: Timeout = 5.seconds

  import akka.pattern.ask

  def receive = {

    case (value: Double, by: String) =>
      log.info(s"filter $id, stream $stream_id received $value by $by")
      count += 1
      transform match {
        case "avg" => avg(value, by)
        case "sum" => sum(value, by)
        case "max" => max(value, by)
        case "min" => min(value, by)
        case "count" => count(value, by)
      }

    case "tick" =>

      // empty the stored collection, and re-capture
      store.clear
      transform match {
        case "count" =>
          // take the first 5 element sorted by value descending
          store ++= acc.toArray.sortWith(_._2 > _._2).take(5).toMap

        case _ => store ++= acc
      }

      // reset the counter and the accumulator
      count = 0
      acc.clear

      // see you "resolution" seconds later
      in(Duration(resolution, SECONDS)) {
        self ! "tick"
      }

    case Get =>
      log.info(s"filter $id, stream $stream_id pushing the data out")

      //      val pipeline: HttpRequest => Future[postResponse] = (
      //          sendReceive
      //          ~> unmarshal[postResponse]
      //        )
      //
      //      val payload = Map(
      //        "_result" -> "300",
      //        "_id" -> "540645e169e5b36a5b001371",
      //        "apiKey" -> "0902f285-8824-4745-8f8b-81f24985fa1b"
      //      )
      //
      //      val response: Future[postResponse] = pipeline(Post("https://dashku.com/api/transmission", payload /* Map("a"->"b","c"->"2.4") */ ))
      //
      //      response onComplete {
      //        case Success(x) =>
      //          log.info("The success ")
      //
      //        case Failure(error) =>
      //          log.error(error, "Couldn't post")
      //      }

//      val payload = """{
//      "_result": 500,
//      "_id": "540645e169e5b36a5b001371",
//      "apiKey": "0902f285-8824-4745-8f8b-81f24985fa1b"
//    }"""
//
//      val actor = actorRefFactory.actorOf(Props(new MyRequestActor("https://dashku.com", 443)), name = "my-request-actor")
//      val response = actor ? HttpRequest(POST, "/api/transmission", Nil, HttpEntity(`application/json`, payload))
//      response.mapTo[postResponse].onComplete {
//        case Success(x) =>
//          log.info("The success ")
//
//        case Failure(error) =>
//          log.error(error, "Couldn't post")
//      }

      sender ! store.toMap

  }

  def in[U](duration: FiniteDuration)(body: => U): Unit =
    actorSystem.scheduler.scheduleOnce(duration)(body)

}


// Actor that manages the lifecycle of a single HTTP connection for a single request
class MyRequestActor(host: String, port: Int) extends Actor with ActorLogging {

  import context.system

  def receive: Receive = {
    case request: HttpRequest =>
      // start by establishing a new HTTP connection
      IO(Http) ! Http.Connect(host, port)
      context.become(connecting(sender, request))
  }

  def connecting(commander: ActorRef, request: HttpRequest): Receive = {
    case _: Http.Connected =>
      // once connected, we can send the request across the connection
      sender ! request
      context.become(waitingForResponse(commander))

    case Http.CommandFailed(Http.Connect(address, _, _, _, _)) =>
      log.warning("Could not connect to {}", address)
      commander ! Status.Failure(new RuntimeException("Connection error"))
      context.stop(self)
  }

  def waitingForResponse(commander: ActorRef): Receive = {
    case response@HttpResponse(status, entity, _, _) =>
      log.info("Connection-Level API: received {} response with {} bytes", status, entity.data.length)
      sender ! Http.Close
      context.become(waitingForClose(commander, response))

    case ev@(Http.SendFailed(_) | Timedout(_)) =>
      log.warning("Received {}", ev)
      commander ! Status.Failure(new RuntimeException("Request error"))
      context.stop(self)
  }

  def waitingForClose(commander: ActorRef, response: HttpResponse): Receive = {
    case ev: Http.ConnectionClosed =>
      log.debug("Connection closed ({})", ev)
      commander ! Status.Success(response.header[HttpHeaders.Server].get.products.head)
      context.stop(self)

    case Http.CommandFailed(Http.Close) =>
      log.warning("Could not close connection")
      commander ! Status.Failure(new RuntimeException("Connection close error"))
      context.stop(self)
  }
}
