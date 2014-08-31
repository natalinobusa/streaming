package com.natalinobusa.streaming.models

object Messages {
  case class DeleteStream(id:Int)
  case class GetStream(id:Int)
  case class ListStreams()
  case class CreateStream()

  case class CreateEvent(stream_id:Int, value:Int)

  case class CreateFilter(stream_id:Int)
  case class ListFilters(stream_id:Int)
  case class DeleteFilter(stream_id:Int, filter_id:Int)
}
