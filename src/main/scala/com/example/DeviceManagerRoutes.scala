import DeviceManager.{RequestAllTemperatures, RequestTrackDevice}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route

import scala.concurrent.duration._
import scala.concurrent.Future

class DeviceManagerRoutes(deviceMangerRepository: ActorRef[DeviceManager.Command])(implicit system: ActorSystem[_])
    extends JsonSupport {
  import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
  import akka.actor.typed.scaladsl.AskPattern.Askable

  implicit val timeout: Timeout = 3.seconds

  lazy val theDeviceManagerRoutes: Route =
    pathPrefix("group" / LongNumber) { groupId =>
      concat(
        path("device" / LongNumber ) { deviceId =>
          concat(
            pathEnd {
              get {
                val operationPerformed: Future[DeviceManager.Command] =
                  deviceMangerRepository.ask(ref => DeviceManager.RequestTrackDevice(groupId.toString, deviceId.toString(), ref))
                onSuccess(operationPerformed) {
                  case _: DeviceManager.DeviceRegistered => complete(
                    HttpEntity(
                      ContentTypes.`text/plain(UTF-8)`,
                      "Device"
                    )
                  )
                  case _ => complete(StatusCodes.InternalServerError)
                }
              }
            }
          )
        },
        path("all" ) {
          get {
            val operationPerformed: Future[DeviceManager.Command] =
              deviceMangerRepository.ask(ref => DeviceManager.RequestDeviceList(requestId = 42, groupId.toString, ref))
            onSuccess(operationPerformed) {
              case x:DeviceManager.ReplyDeviceList         => complete(
                HttpEntity(
                  ContentTypes.`text/plain(UTF-8)`,
                  x.ids.toString
                )
              )
              case _ => complete(StatusCodes.InternalServerError)
            }
          }
        }
      )
    }
}