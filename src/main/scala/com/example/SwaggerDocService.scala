package com.example.swagger

import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import com.example.{DeviceManagerRoutes, DeviceRoutes}
import io.swagger.v3.oas.models.ExternalDocumentation

object SwaggerDocService extends SwaggerHttpService {
  override val apiClasses = Set(classOf[DeviceManagerRoutes], classOf[DeviceRoutes])
  override val host = "localhost:8080"
  override val info: Info = Info(version = "1.0")
  override val apiDocsPath = "api-docs"
}