package com.natalinobusa.streaming

import akka.actor.{Props, Actor, ActorLogging}
import com.natalinobusa.streaming.models.Messages.Get

import scala.collection.mutable

object FilterActor {
  def props(id:Int, stream_id:Int, resolution:Int, transform:String): Props =
    Props(new FilterActor(id, stream_id, resolution, transform))
}

class FilterActor(id:Int, stream_id:Int, resolution:Int, transform:String) extends Actor with ActorLogging {

  def actorRefFactory = context

  var count = 0
  val acc   = mutable.HashMap.empty[String, Double ]
  val store = mutable.HashMap.empty[String, Double ]

  def sum(value:Double, key:String) = {
    val curr = acc.getOrElseUpdate(key,0)
    acc(key) = curr+value
  }

  def avg(value:Double, key:String) = {
    // iterative arith mean
    val curr = acc.getOrElseUpdate(key,value)
    acc(key) = curr + (curr - value)/count
  }

  def max(value:Double, key:String) = {
    val curr = acc.getOrElseUpdate(key,value)
    acc(key) = if (curr>value) curr else value
  }

  def min(value:Double, key:String) = {
    val curr = acc.getOrElseUpdate(key,value)
    acc(key) = if (curr>value) value else curr
  }

  def count(value:Double, key:String) = {
    val curr = acc.getOrElseUpdate(key,0)
    acc(key) = curr+1
  }

  def receive = {

    case (value:Double, by:String)   =>
      count += 1
      transform match {
        case "avg"   => sum(value, by)
        case "sum"   => sum(value, by)
        case "max"   => max(value, by)
        case "min"   => min(value, by)
        case "count" => count(value, by)
      }

      log.info(s"filter $id, stream $stream_id received $value by $by")

    case Get =>
      log.info(s"filter $id, stream $stream_id pushing the data out")
      sender ! acc.toMap
  }

}
