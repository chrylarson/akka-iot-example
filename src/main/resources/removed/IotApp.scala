package com.example.removed

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, concat, get, path, pathPrefix}
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.io.StdIn
import scala.util.Random

object IotApp {
  def main(args: Array[String]): Unit = {
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val DeviceManagerActor = context.spawn(DeviceManager(), "DeviceManagerActor")
      context.watch(DeviceManagerActor)

      val anotherActor = context.spawn(DeviceManager(), "anotherActor")
      context.watch(anotherActor)

      // Create ActorStystem and top level supervisor
    implicit val system = ActorSystem[Nothing](IotSupervisor(), "iot-system")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.executionContext

    // streams are re-usable so we can define it here
    // and use it for every request
    val numbers = Source.fromIterator(() =>
      Iterator.continually(Random.nextInt()))

    val route =
      concat(
        path("ping") {
          get {
            complete(
              HttpEntity(
                ContentTypes.`text/plain(UTF-8)`,
                // transform each number to a chunk of bytes
                "pong"
              )
            )
          }
        },
        path("random") {
          get {
            complete(
              HttpEntity(
                ContentTypes.`text/plain(UTF-8)`,
                // transform each number to a chunk of bytes
                numbers.map(n => ByteString(s"$n\n"))
              )
            )
          }
        },
        pathPrefix("group" / LongNumber) { groupId =>
          concat(
            path("device" / LongNumber ) { deviceId =>
              get {
                DeviceManagerActor ! RequestTrackDevice(groupId.toString, deviceId.toString(), anotherActor)
                complete(
                  HttpEntity(
                    ContentTypes.`text/plain(UTF-8)`,
                    "Device"
                  )
                )
              }
            },
            path("all" ) {
              get {
                DeviceManagerActor ! RequestAllTemperatures(requestId = 42, groupId.toString, anotherActor)
                complete(
                  HttpEntity(
                    ContentTypes.`text/plain(UTF-8)`,
                    "Device"
                  )
                )
              }
            }
          )
        })

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done


      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
  }

}
