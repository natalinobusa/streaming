package com.natalinobusa.streaming

import models.Resources._
import models.Messages._

import akka.actor._

import spray.json._
import DefaultJsonProtocol._

import scala.collection.mutable

object StreamActor {
  def props(id: Int): Props = Props(new StreamActor(id))
}

class StreamActor(stream_id: Int) extends Actor with ActorLogging {

  def actorRefFactory = context

  val directory  = mutable.HashMap.empty[Int, (ActorPath, Filter) ]
  var count= 0

  def receive = {
    case CreateFilter(resolution, field, transform, groupBy) =>
      count += 1
      val id = count
      val actor = actorRefFactory.actorOf(FilterActor.props(id, stream_id, resolution, transform), s"filter-$id")
      val filter = Filter(id, stream_id, resolution, field, transform, groupBy)
      directory += (id -> (actor.path, filter))
      log.info("created filterActor {} {}", actor.path.toString, id)
      sender ! Some(filter)

    case Delete(id) =>
      directory.get(id).map(e => actorRefFactory.actorSelection(e._1) ! PoisonPill)
      directory -= id
      sender ! true

    case Delete =>
      directory.foreach( e => actorRefFactory.actorSelection(e._2._1) ! PoisonPill)
      directory.clear()
      sender ! true

    case List =>
      sender ! directory.values.map(e => e._2).toList

    case  Get(id) =>
      sender ! { directory.get(id).map( e => Some(e._2) ) }

    case  GetActorPath(id) =>
      sender ! directory.get(id).map(e => e._1)

    case CreateEvent(value) =>
      log.info(s"got message $value on stream $stream_id ...")

      val jsonAst = JsonParser(value)

      directory.foreach {
        e => {
          val filterParams     = e._2._2
          val filterActorPath  = e._2._1

          jsonAst.asJsObject.getFields(filterParams.field, filterParams.group_by) match {
            case Seq(JsNumber(value), JsString(by)) =>  actorRefFactory.actorSelection(filterActorPath) ! (value.toLong, by.toString)
            case _ =>
          }

        }
      }
      sender ! true
  }
}
