package com.example

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs.{GET, Path, Produces}
import javax.ws.rs.core.MediaType

import scala.concurrent.duration._
import scala.concurrent.Future


class DeviceManagerRoutes(deviceMangerRepository: ActorRef[DeviceManager.Command])(implicit system: ActorSystem[_])
    extends JsonSupport {
  import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
  import akka.actor.typed.scaladsl.AskPattern.Askable

  implicit val timeout: Timeout = 3.seconds

  lazy val theDeviceManagerRoutes: Route =
    concat(getDevice, listDevices)

  @GET
  @Path("/group/{groupId}/device/{deviceId}")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Starts Device Actor",
    parameters = Array(
      new Parameter(name = "groupId", in = ParameterIn.PATH, description = "Group ID"),
      new Parameter(name = "deviceId", in = ParameterIn.PATH, description = "Device ID")
    ),
    description = "Starts device actor if it isn't already running",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Device",
        content = Array(new Content(schema = new Schema(implementation = classOf[DeviceManagerRoutes])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def getDevice: Route =
      path("group" / LongNumber / "device" / LongNumber ) { (groupId, deviceId) =>
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
      }

  @GET
  @Path("/group/{groupId}/all")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Lists Device Actors",
    parameters = Array(
      new Parameter(name = "groupId", in = ParameterIn.PATH, description = "Group ID")
    ),
    description = "Lists all device actors",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Device",
        content = Array(new Content(schema = new Schema(implementation = classOf[DeviceManagerRoutes])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def listDevices: Route =
    path("group" / LongNumber / "all" ) { groupId =>
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
}