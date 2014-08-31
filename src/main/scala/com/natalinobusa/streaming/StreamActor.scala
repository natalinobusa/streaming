package com.natalinobusa.streaming

import models.Resources._
import models.Messages._

import akka.actor.{Actor, ActorLogging, ActorPath, Props}

import scala.collection.mutable

class StreamActor extends Actor with ActorLogging {

  def actorRefFactory = context

  val directory  = mutable.HashMap.empty[Int, ActorPath]
  var count= 0

  def receive = {
    case CreateFilter(stream_id) =>
      count += 1
      val actor = actorRefFactory.actorOf(Props[FilterActor], s"filter-$count")
      directory += (count -> actor.path)
      log.info("created filterActor {} {}", actor.path.toString, count)
      sender ! Some(Filter(count, stream_id, 1, "count", None))
  }
}
