import DeviceManager.{DeviceRegistered, RequestTrackDevice}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.scaladsl.Source
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._

import scala.util.Random
import scala.io.StdIn

object IotApp {
  def main(args: Array[String]): Unit = {
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val DeviceManagerActor = context.spawn(DeviceManager(), "DeviceManagerActor")
      context.watch(DeviceManagerActor)

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
        path("device") {
          get {
            DeviceManagerActor ! RequestTrackDevice("group1", "device1", DeviceManagerActor)
            complete(
              HttpEntity(
                ContentTypes.`text/plain(UTF-8)`,
                "Device"
              )
            )
          }
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


