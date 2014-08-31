package com.natalinobusa.streaming
import models.Messages._
import models.Resources._

import akka.actor._

import scala.collection.mutable

class StreamsActor extends Actor with ActorLogging {

  def actorRefFactory = context

  val directory  = mutable.HashMap.empty[Int, ActorPath]
  var count= 0

  def receive = {
    case CreateStream =>
      count += 1
      val actor = actorRefFactory.actorOf(Props[StreamActor], s"stream-$count")
      directory += (count -> actor.path)
      sender ! Stream(count)

    case ListStreams =>
      val ids = directory.keySet.toList
      sender ! ids.map(id => Stream(id))

    case  DeleteStream(stream_id) =>
      directory.get(stream_id).map(path => actorRefFactory.actorSelection(path) ! PoisonPill)
      directory -= stream_id
      sender ! true

    case  GetStream(stream_id) =>
      sender ! { if (directory.isDefinedAt(stream_id)) Some(stream_id) else None }

    case  CreateEvent(stream_id, value) =>
      directory.get(stream_id) match {
        case Some(actorPath) =>
          log.info(s"passing $value to actor $stream_id")
          actorRefFactory.actorSelection(actorPath) ! value
          sender ! true

        case None => sender ! false
      }

    case  CreateFilter(stream_id) =>
      directory.get(stream_id) match {
        case Some(actorPath) =>
          log.info(s"forwarding to $stream_id")
          val streamActor = actorRefFactory.actorSelection(actorPath)
          streamActor.tell(CreateFilter(stream_id), sender)

        case None => sender ! None
      }

  }
}
