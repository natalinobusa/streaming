package com.natalinobusa.streaming

import scala.concurrent.duration._
import scala.util.{Failure, Success}

// the service, actors and paths

import akka.actor.{Props, Actor}
import akka.pattern.ask
import akka.util.Timeout

import spray.routing.HttpService
import spray.can.Http
import spray.util._
import spray.http._
import MediaTypes._


// our models

import models.Resources._
import models.Messages._
import models.Conversions._

// marshalling resources to json

import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
import models.JsonConversions._

class ApiStreamingServiceActor extends Actor with ApiStreamingService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // create the streams router actor
  val router = actorRefFactory.actorOf(Props[StreamsActor], "streams")

  // this actor only runs our route, but you could add
  // other things here, like request stream processing,
  // timeout handling or alternative handler registration
  def receive = runRoute(serviceRoute)
}

// Routing embedded in the actor
trait ApiStreamingService extends HttpService {

  implicit def executionContext = actorRefFactory.dispatcher

  implicit val timeout = Timeout(1.seconds)

  def apiStreamsActor = actorRefFactory.actorSelection("/user/api/streams")

  val serviceRoute = {
    pathPrefix("api" / "streams") {
      pathEnd {
        get {
          ctx => apiStreamsActor.ask(ListStreams).mapTo[List[Stream]]
            .onSuccess { case resources => ctx.complete(toStreamRest(resources))}
        } ~
          post {
            ctx => apiStreamsActor.ask(CreateStream).mapTo[Stream]
              .onSuccess { case resource => ctx.complete(toStreamRest(resource))}
        } ~
          (delete | head | patch) {
          complete(HttpResponse(StatusCodes.MethodNotAllowed))
        }
      } ~
      pathPrefix(IntNumber) { stream_id =>
        pathEnd {
          get {
            ctx => apiStreamsActor.ask(GetStream(stream_id)).mapTo[Option[Stream]]
              .onSuccess {
              case Some(stream) => ctx.complete(toStreamRest(stream))
              case None => ctx.complete(HttpResponse(StatusCodes.NotFound))
            }
          } ~
          delete {
            ctx => apiStreamsActor.ask(DeleteStream(stream_id)).mapTo[Boolean]
              .onSuccess {
                 case _ => ctx.complete(HttpResponse(StatusCodes.OK))
            }
          }
        } ~
        pathPrefix("in" / "events") {
          pathEnd{
            post {
              ctx => apiStreamsActor.ask(CreateEvent(stream_id, 42)).mapTo[Boolean]
                .onSuccess {
                case true  => ctx.complete(HttpResponse(StatusCodes.OK))
                case false => ctx.complete(HttpResponse(StatusCodes.BadRequest))
              }
            }
          } ~
          pathPrefix("filters") {
            pathEnd {
              //              get {
              //                ctx => apiStreamsActor.ask(ListFilters(id)).mapTo[List[Filter]]
              //                  .onSuccess { case resources => ctx.complete(toFilterRest(resources))}
              //              }
              //              ~
              post {
                ctx => apiStreamsActor.ask(CreateFilter(stream_id)).mapTo[Option[Filter]]
                  .onSuccess {
                    case Some(resource) => ctx.complete(toFilterRest(resource))
                    case None => ctx.complete(HttpResponse(StatusCodes.BadRequest))
                }
              }
              //              ~
              //                (delete | head | patch) {
              //                  complete(HttpResponse(StatusCodes.MethodNotAllowed))
              //                }
              //            } ~

            }
          }
        }
      }
    }
  }
}

