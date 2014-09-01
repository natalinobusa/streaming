package com.natalinobusa.streaming


import scala.concurrent.duration._

// the service, actors and paths

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import spray.routing.{HttpService}
import spray.can.Http
import spray.util._
import spray.http._
import MediaTypes._


// our models

import models.Resources._
import models.Messages._
import models.Conversions._

// marshalling responses to json
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

  def streamsActor = actorRefFactory.actorSelection("/user/api/streams")

  val ingestRoute = {
    pathPrefix("streams" / IntNumber / "in" / "events") { stream_id =>
      pathEnd {

        // validate the path, and expose the actor
        onSuccess(streamsActor.ask(GetActorPath(stream_id)).mapTo[Option[ActorPath]]) {
          streamActorPathOption => validate(streamActorPathOption.isDefined, "") {
            provide(streamActorPathOption.orNull) {
              streamActorPath => {

                // the actual post
                post {
                  entity(as[String]) { s =>
                    ctx => actorRefFactory.actorSelection(streamActorPath).ask(CreateEvent(s)).mapTo[Boolean]
                      .onSuccess { case true => ctx.complete(HttpResponse(StatusCodes.OK))}
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  def filterRoute(streamActorPath: ActorPath) = {
    pathPrefix(IntNumber) { filter_id =>
      pathEnd {
        get {
          ctx => actorRefFactory.actorSelection(streamActorPath).ask(Get(filter_id)).mapTo[Option[Filter]]
            .onSuccess {
            case Some(filter) => ctx.complete(toFilterRest(filter))
            case None => ctx.complete(HttpResponse(StatusCodes.NotFound))
          }
        } ~
          delete {
            ctx => actorRefFactory.actorSelection(streamActorPath).ask(Delete(filter_id)).mapTo[Boolean]
              .onSuccess { case _ => ctx.complete(HttpResponse(StatusCodes.OK))}
          }
      }
    }
  }

  val filtersRoute = {
    pathPrefix("streams" / IntNumber / "in" / "events" / "filters") { stream_id =>
      pathEnd {

        // validate the path, and expose the actor
        onSuccess(streamsActor.ask(GetActorPath(stream_id)).mapTo[Option[ActorPath]]) {
          streamActorPathOption => validate(streamActorPathOption.isDefined, "") {
            provide(streamActorPathOption.orNull) {
              streamActorPath => {

                get {
                    ctx => actorRefFactory.actorSelection(streamActorPath).ask(List).mapTo[List[Filter]]
                      .onSuccess { case filters => ctx.complete(toFilterRest(filters))}
                  } ~
                  post {
                    entity( as[CreateFilter] ) { filterDefinition =>
                      ctx => actorRefFactory.actorSelection(streamActorPath).ask(filterDefinition).mapTo[Option[Filter]]
                        .onSuccess { case Some(filter) => ctx.complete(toFilterRest(filter))}
                    }
                  } ~
                  delete {
                    ctx => actorRefFactory.actorSelection(streamActorPath).ask(CreateStream).mapTo[Boolean]
                      .onSuccess { case _ => ctx.complete(HttpResponse(StatusCodes.OK))}
                  } ~
                  filterRoute(streamActorPath)
              }
            }
          }
        }
      }
    }
  }

  val streamRoute = {
    pathPrefix(IntNumber) { stream_id =>
      pathEnd {
        get {
          ctx => streamsActor.ask(Get(stream_id)).mapTo[Option[Stream]]
            .onSuccess {
              case Some(stream) => ctx.complete(toStreamRest(stream))
              case None => ctx.complete(HttpResponse(StatusCodes.NotFound))
          }
        } ~
          delete {
            ctx => streamsActor.ask(Delete(stream_id)).mapTo[Boolean]
              .onSuccess { case _ => ctx.complete(HttpResponse(StatusCodes.OK))}
          }
      }
    }
  }

  val streamsRoute = {
    pathPrefix("streams") {
      pathEnd {
        get {
          ctx => streamsActor.ask(List).mapTo[List[Stream]]
            .onSuccess { case streams => ctx.complete(toStreamRest(streams))}
        } ~
          post {
            ctx => streamsActor.ask(CreateStream).mapTo[Option[Stream]]
              .onSuccess { case Some(stream) => ctx.complete(toStreamRest(stream))}
          } ~
          (delete | head | patch) {
            complete(HttpResponse(StatusCodes.MethodNotAllowed))
          }
      } ~
      streamRoute
    }
  }

  val serviceRoute = {
    pathPrefix("api") {
      streamsRoute ~
      ingestRoute ~
      filtersRoute
    }
  }

}
