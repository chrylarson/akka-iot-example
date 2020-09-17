package com.example

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._
import spray.json.DeserializationException
import spray.json.JsString
import spray.json.JsValue
import spray.json.RootJsonFormat

case class SensorData(value: Double)

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._
  implicit val sensorData = jsonFormat1(SensorData)
}