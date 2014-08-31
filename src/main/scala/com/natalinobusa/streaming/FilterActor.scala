package com.natalinobusa.streaming

import akka.actor.{Actor, ActorLogging}

class FilterActor extends Actor with ActorLogging {

  def actorRefFactory = context

  def receive = {
    case value:Int => log.info(s"received $value")
  }

}
