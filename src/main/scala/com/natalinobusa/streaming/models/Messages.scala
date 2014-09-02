package com.natalinobusa.streaming.models

object Messages {
  // generic messages for resources
  case class  Get(id:Int)
  case class  Head(id:Int)
  case class  Delete(id:Int)
  case object Get
  case object List
  case object Create
  case object Delete

  // create resources
  case class CreateEvent(value:String)
  case class CreateFilter(resolution:Int, field: String, transform:String, group_by:String)
  case class CreateStream()

  //Actors: internal routing and selection
  case class GetActorPath(id:Int)

}
