package com.natalinobusa.streaming.models

import com.natalinobusa.streaming.models.Resources.Action
import com.natalinobusa.streaming.models.Rest._
import com.natalinobusa.streaming.models.Messages.{postResponse, CreateFilter}

import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.httpx.SprayJsonSupport.sprayJsonMarshaller

object JsonConversions extends DefaultJsonProtocol with SprayJsonSupport {
  //filter definition
  implicit val impAction       = jsonFormat2(Action)
  implicit val impCreateFilter = jsonFormat5(CreateFilter)

  //filter rest
  implicit val impFilterHyper = jsonFormat3(FilterHyper)
  implicit val impFilter      = jsonFormat8(FilterRest)

  //stream rest
  implicit val impStreamHyper = jsonFormat3(StreamHyper)
  implicit val impStream      = jsonFormat2(StreamRest)

  // client
  implicit val impPostResponse = jsonFormat1(postResponse)

}
