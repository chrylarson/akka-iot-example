package com.example.removed

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

class JobRoutes(buildJobRepository: ActorRef[JobRepository.Command])(implicit system: ActorSystem[_]) extends JsonSupport {

  import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}

  // asking someone requires a timeout and a scheduler, if the timeout hits without response
  // the ask is failed with a TimeoutException
  implicit val timeout: Timeout = 3.seconds

  lazy val theJobRoutes: Route =
    pathPrefix("jobs") {
      concat(
        pathEnd {
          concat(
            post {
              entity(as[JobRepository.Job]) { job =>
                val operationPerformed: Future[JobRepository.Response] =
                  buildJobRepository.ask(JobRepository.AddJob(job, _))
                onSuccess(operationPerformed) {
                  case JobRepository.OK         => complete("Job added")
                  case JobRepository.KO(reason) => complete(StatusCodes.InternalServerError -> reason)
                }
              }
            },
            delete {
              val operationPerformed: Future[JobRepository.Response] =
                buildJobRepository.ask(JobRepository.ClearJobs(_))
              onSuccess(operationPerformed) {
                case JobRepository.OK         => complete("Jobs cleared")
                case JobRepository.KO(reason) => complete(StatusCodes.InternalServerError -> reason)
              }
            }
          )
        },
        (get & path(LongNumber)) { id =>
          val maybeJob: Future[Option[JobRepository.Job]] =
            buildJobRepository.ask(JobRepository.GetJobById(id, _))
          rejectEmptyResponse {
            complete(maybeJob)
          }
        }
      )
    }
}