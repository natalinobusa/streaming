package com.natalinobusa.streaming

import akka.actor.{Props, Actor, ActorLogging}

object FilterActor {
  def props(id:Int, stream_id:Int, resolution:Int, transform:String): Props =
    Props(new FilterActor(id, stream_id, resolution, transform))
}

class FilterActor(id:Int, stream_id:Int, resolution:Int, transform:String) extends Actor with ActorLogging {

  def actorRefFactory = context

  def receive = {
    case (value:Long, by:String)   => log.info(s"filter $id, stream $stream_id received $value by $by")
  }

}
