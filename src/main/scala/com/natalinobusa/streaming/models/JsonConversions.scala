package com.natalinobusa.streaming.models

import com.natalinobusa.streaming.models.Rest._
import spray.json.DefaultJsonProtocol

object JsonConversions extends DefaultJsonProtocol {
  //stream
  implicit val impStreamHyper = jsonFormat3(StreamHyper)
  implicit val impStream      = jsonFormat2(StreamRest)

  //filter
  implicit val impFilterHyper = jsonFormat3(FilterHyper)
  implicit val impFilter      = jsonFormat6(FilterRest)
}