package com.example.removed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object JobRepository {

  // Definition of the a build job and its possible status values
  sealed trait Status
  object Successful extends Status
  object Failed extends Status

  final case class Job(id: Long, projectName: String, status: Status, duration: Long)

  // Trait defining successful and failure responses
  sealed trait Response
  case object OK extends Response
  final case class KO(reason: String) extends Response

  // Trait and its implementations representing all possible messages that can be sent to this Behavior
  sealed trait Command
  final case class AddJob(job: Job, replyTo: ActorRef[Response]) extends Command
  final case class GetJobById(id: Long, replyTo: ActorRef[Option[Job]]) extends Command
  final case class ClearJobs(replyTo: ActorRef[Response]) extends Command

  // This behavior handles all possible incoming messages and keeps the state in the function parameter
  def apply(jobs: Map[Long, Job] = Map.empty): Behavior[Command] = Behaviors.receiveMessage {
    case AddJob(job, replyTo) if jobs.contains(job.id) =>
      replyTo ! KO("Job already exists")
      Behaviors.same
    case AddJob(job, replyTo) =>
      replyTo ! OK
      JobRepository(jobs.+(job.id -> job))
    case GetJobById(id, replyTo) =>
      replyTo ! jobs.get(id)
      Behaviors.same
    case ClearJobs(replyTo) =>
      replyTo ! OK
      JobRepository(Map.empty)
  }

}
