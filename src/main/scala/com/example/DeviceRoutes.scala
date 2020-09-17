package com.example

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs.{Consumes, GET, POST, Path, Produces}
import javax.ws.rs.core.MediaType

import scala.concurrent.Future
import scala.concurrent.duration._

class DeviceRoutes(deviceRepo: ActorRef[DeviceManager.Command])(implicit system: ActorSystem[_])
  extends JsonSupport {
  import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
  import akka.actor.typed.scaladsl.AskPattern.Askable

  implicit val timeout: Timeout = 3.seconds

  lazy val theDeviceRoutes: Route =
    concat(publishData, readData)

  @POST
  @Path("/group/{groupId}/device/{deviceId}/data")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Publish data from sensor",
    parameters = Array(
      new Parameter(name = "deviceId", in = ParameterIn.PATH, description = "Device ID")
    ),
    description = "Accepts JSON data from devices",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Accepted",
        content = Array(new Content(schema = new Schema(implementation = classOf[DeviceRoutes])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def publishData: Route =
    path("group" / LongNumber / "device" / LongNumber / "data" ) { ( groupId, deviceId ) =>
      pathEnd {
        post {
          entity(as[SensorData]) { data =>
            val deviceActor: Future[DeviceManager.Command] =
              deviceRepo.ask(ref => DeviceManager.RequestTrackDevice(groupId.toString, deviceId.toString(), ref))
            onSuccess(deviceActor) {
              case actor: DeviceManager.DeviceRegistered => {
                  actor.device ! Device.RecordTemperature(requestId = 42, data.value, actor.device)
                complete(
                  HttpEntity(
                    ContentTypes.`text/plain(UTF-8)`,
                    "Success"
                  )
                )
              }
              case _ => complete(StatusCodes.InternalServerError)
            }
          }
        }
      }
    }

  @GET
  @Path("/group/{groupId}/device/{deviceId}/data")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Read data from sensor",
    parameters = Array(
      new Parameter(name = "deviceId", in = ParameterIn.PATH, description = "Device ID")
    ),
    description = "Returns the data from the sensor",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Accepted",
        content = Array(new Content(schema = new Schema(implementation = classOf[DeviceRoutes])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def readData: Route =
    path("group" / LongNumber / "device" / LongNumber / "data" ) { ( groupId, deviceId ) =>
      pathEnd {
        get {
          val deviceActor: Future[DeviceManager.Command] =
            deviceRepo.ask(ref => DeviceManager.RequestTrackDevice(groupId.toString, deviceId.toString(), ref))
          onSuccess(deviceActor) {
            case actor: DeviceManager.DeviceRegistered => {
              val response: Future[Device.Command] = actor.device.ask(ref => Device.ReadTemperature(requestId = 42, ref))
              onSuccess(response) {
                case res: Device.RespondTemperature => {
                  complete(
                    HttpEntity(
                      ContentTypes.`text/plain(UTF-8)`,
                      "Device" + res.toString
                    )
                  )}
                case _ => complete(StatusCodes.InternalServerError)
              }
            }
            case _ => complete(StatusCodes.InternalServerError)
          }
        }
      }
    }
}