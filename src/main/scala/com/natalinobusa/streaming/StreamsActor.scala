package com.natalinobusa.streaming
import models.Messages._
import models.Resources._

import akka.actor._

import scala.collection.mutable

class StreamsActor extends Actor with ActorLogging {

  def actorRefFactory = context

  val directory  = mutable.HashMap.empty[Int, (ActorPath, Stream) ]
  var count= 0

  def receive = {
    case CreateStream =>
      count += 1
      val id  = count
      val stream = Stream(id)
      val actor = actorRefFactory.actorOf(StreamActor.props(id), s"stream-$id")
      directory += (id -> (actor.path, stream))
      sender ! Some(stream)

    case List =>
      sender ! directory.values.map(e => e._2).toList

    case Delete(id) =>
      directory.get(id).map(e => actorRefFactory.actorSelection(e._1) ! PoisonPill)
      directory -= id
      sender ! true

    case  Get(id) =>
      sender ! { directory.get(id).map( e => Some(e._2) ) }

    case  GetActorPath(id) =>
      sender ! directory.get(id).map(e => e._1)

  }
}
