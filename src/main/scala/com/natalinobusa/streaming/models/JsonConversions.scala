package com.natalinobusa.streaming.models

import com.natalinobusa.streaming.models.Rest._
import com.natalinobusa.streaming.models.Messages.{postResponse, CreateFilter}

import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol

object JsonConversions extends DefaultJsonProtocol with SprayJsonSupport {
  //stream
  implicit val impStreamHyper = jsonFormat3(StreamHyper)
  implicit val impStream      = jsonFormat2(StreamRest)

  //filter
  implicit val impFilterHyper = jsonFormat3(FilterHyper)
  implicit val impFilter      = jsonFormat7(FilterRest)

  //filter definition
  implicit val impCreateFilter = jsonFormat4(CreateFilter)

  // client
  implicit val impPostResponse = jsonFormat1(postResponse)

}
