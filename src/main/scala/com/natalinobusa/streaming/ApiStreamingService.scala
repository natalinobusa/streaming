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

  // just a few handy shortcut
  def Ask(a: ActorPath, msg:Any) =  actorRefFactory.actorSelection(a).ask(msg)
  def Ask(a: String, msg:Any)    =  actorRefFactory.actorSelection(a).ask(msg)
  def Ask(a: ActorSelection, msg: Any) = a.ask(msg)

  val ingestRoute = {
    pathPrefix("streams" / IntNumber / "in" / "events") { stream_id =>
      pathEnd {

        // validate the path, and expose the actor
        onSuccess( Ask( streamsActor, GetActorPath(stream_id) ).mapTo[Option[ActorPath]]) {
          streamActorPathOption => validate(streamActorPathOption.isDefined, "") {
            provide(streamActorPathOption.orNull) {
              streamActorPath => {

                // the actual post
                post {
                  entity(as[String]) { s =>
                    ctx => Ask( streamActorPath, CreateEvent(s) ).mapTo[Boolean]
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
          ctx => Ask( streamActorPath, Get(filter_id) ).mapTo[Option[Filter]]
            .onSuccess {
            case Some(filter) => ctx.complete(toFilterRest(filter))
            case None => ctx.complete(HttpResponse(StatusCodes.NotFound))
          }
        } ~
          delete {
            ctx => Ask( streamActorPath, Delete(filter_id) ).mapTo[Boolean]
              .onSuccess { case _ => ctx.complete(HttpResponse(StatusCodes.OK))}
          }
      }
    }
  }

  val filtersRoute = {
    pathPrefix("streams" / IntNumber / "in" / "events" / "filters") { stream_id =>

      // validate the path, and expose the actor
      onSuccess(Ask(streamsActor, GetActorPath(stream_id)).mapTo[Option[ActorPath]]) {
        streamActorPathOption => validate(streamActorPathOption.isDefined, "") {
          provide(streamActorPathOption.orNull) {
            streamActorPath => {

              pathEnd {
                get {
                  ctx => Ask(streamActorPath, List).mapTo[List[Filter]]
                    .onSuccess { case filters => ctx.complete(toFilterRest(filters))}
                } ~
                  post {
                    entity(as[CreateFilter]) { filterDefinition =>
                      ctx => Ask(streamActorPath, filterDefinition).mapTo[Option[Filter]]
                        .onSuccess { case Some(filter) => ctx.complete(toFilterRest(filter))}
                    }
                  } ~
                  delete {
                    ctx => Ask(streamActorPath, CreateStream).mapTo[Boolean]
                      .onSuccess { case _ => ctx.complete(HttpResponse(StatusCodes.OK))}
                  }
              } ~
              filterRoute(streamActorPath)
            }
          }
        }
      }
    }
  }

  val outputRoute = {
    pathPrefix("streams" / IntNumber / "in" / "events" / "filtered_by" / IntNumber / "out" / "events") { (stream_id, filter_id) =>

      // validate the path, and expose the actor
      onSuccess(Ask(streamsActor, GetActorPath(stream_id)).mapTo[Option[ActorPath]]) {
        streamActorPathOption => validate(streamActorPathOption.isDefined, "") {
          provide(streamActorPathOption.orNull) {
            streamActorPath => {

              onSuccess(Ask(streamActorPath, GetActorPath(filter_id)).mapTo[Option[ActorPath]]) {
                filterActorPathOption => validate(filterActorPathOption.isDefined, "") {
                  provide(filterActorPathOption.orNull) {
                    filterActorPath => {

                      pathEnd {
                        get {
                          ctx => Ask(filterActorPath, Get).mapTo[ Map[String,Double] ]
                            .onSuccess { case result => ctx.complete(result) }
                        }
                      }
                    }
                  }
                }
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
          ctx => Ask(streamsActor, Get(stream_id)).mapTo[Option[Stream]]
            .onSuccess {
              case Some(stream) => ctx.complete(toStreamRest(stream))
              case None => ctx.complete(HttpResponse(StatusCodes.NotFound))
          }
        } ~
          delete {
            ctx => Ask(streamsActor, Delete(stream_id)).mapTo[Boolean]
              .onSuccess { case _ => ctx.complete(HttpResponse(StatusCodes.OK))}
          }
      }
    }
  }

  val streamsRoute = {
    pathPrefix("streams") {
      pathEnd {
        get {
          ctx => Ask(streamsActor,List).mapTo[List[Stream]]
            .onSuccess { case streams => ctx.complete(toStreamRest(streams))}
        } ~
          post {
            ctx => Ask(streamsActor,CreateStream).mapTo[Option[Stream]]
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
      filtersRoute ~
      outputRoute
    }
  }

}
