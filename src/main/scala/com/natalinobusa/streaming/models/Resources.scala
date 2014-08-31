package com.natalinobusa.streaming.models

object Resources {
  case class Stream(id: Int)
  case class Filter(id: Int, stream_id:Int, resolution: Int, transform: String, group_by: Option[String])
}

object Rest {
  case class StreamHyper(uri: String, input: String, filters: String)
  case class StreamRest(id: Int, links: StreamHyper)

  case class FilterHyper(uri: String, input: String, output: String)
  case class FilterRest(id: Int, stream_id: Int, resolution: Int, transform: String, group_by: Option[String], links:FilterHyper)
}

import com.natalinobusa.streaming.models.Resources._
import com.natalinobusa.streaming.models.Rest._

object Conversions {
  def toStreamRest(resource: Stream) = {
    val rootUrl = "http://localhost:8800/api"
    val id  = resource.id

    val links = StreamHyper(
      s"$rootUrl/streams/$id",
      s"$rootUrl/streams/$id/in/events",
      s"$rootUrl/streams/$id/in/events/filters"
    )

    StreamRest(id, links)
  }

  def toStreamRest(resources: List[Stream]): List[StreamRest]  = resources.map( toStreamRest(_))


  def toFilterRest(resource: Filter) = {
    val rootUrl = "http://localhost:8800/api"
    val id  = resource.id

    val links = FilterHyper(
      s"$rootUrl/streams/${resource.stream_id}/in/events/filters/${resource.id}",
      s"$rootUrl/streams/${resource.stream_id}/in/events",
      s"$rootUrl/streams/${resource.stream_id}/in/events/filters/${resource.id}/out/events"
    )

    FilterRest(
      resource.id,
      resource.stream_id,
      resource.resolution,
      resource.transform,
      resource.group_by,
      links)
  }

  def toFilterRest(resources: List[Filter]): List[FilterRest]  = resources.map( toFilterRest(_))

}
