package it.unibo.citytwin.rivermonitor.actors.rivermonitor

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import scala.util.Success
import concurrent.duration.DurationInt
import it.unibo.citytwin.core.actors.{
  AskResourcesToMainstay,
  ResourceActor,
  ResourceActorCommand,
  ResourcesFromMainstayResponse
}
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.*
import it.unibo.citytwin.rivermonitor.model.RiverMonitor
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.Resource
import it.unibo.citytwin.core.model.ResourceType.{Act, Sense}

trait RiverMonitorActorCommand

/** A message representing a periodic tick event for the RiverMonitorActor. This is used to trigger
  * the RiverMonitorActor to perform periodic tasks.
  */
case class Tick(resourcesToCheck: Set[String]) extends Serializable with RiverMonitorActorCommand

case class AdaptedResourcesStateResponse(resources: Set[Resource])
    extends Serializable
    with RiverMonitorActorCommand

object RiverMonitorActor:
  def apply(
      riverMonitor: RiverMonitor,
      resourcesToCheck: Set[String]
  ): Behavior[RiverMonitorActorCommand] =
    Behaviors.setup[RiverMonitorActorCommand] { ctx =>
      implicit val timeout: Timeout = 3.seconds
      val resourceActor             = ctx.spawnAnonymous(ResourceActor())
      val riverMonitorStateActor =
        ctx.spawnAnonymous(RiverMonitorStateActor(riverMonitor, resourceActor))
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick(resourcesToCheck), 1.seconds)
        RiverMonitorActorLogic(ctx, riverMonitorStateActor, resourceActor, riverMonitor)
      }
    }

  private def RiverMonitorActorLogic(
      ctx: ActorContext[RiverMonitorActorCommand],
      riverMonitorStateActor: ActorRef[RiverMonitorStateActorCommand],
      resourceActor: ActorRef[ResourceActorCommand],
      riverMonitor: RiverMonitor
  ): Behavior[RiverMonitorActorCommand] =
    implicit val timeout: Timeout = 3.seconds
    Behaviors.receiveMessage {
      case Tick(resourcesToCheck) => {
        ctx.log.debug("Received Tick")
        ctx.ask(resourceActor, ref => AskResourcesToMainstay(ref, resourcesToCheck)) {
          case Success(ResourcesFromMainstayResponse(resources: Set[Resource])) =>
            AdaptedResourcesStateResponse(resources)
          case _ => {
            ctx.log.debug("Resources not received. Actor is unreachable.")
            AdaptedResourcesStateResponse(Set())
          }
        }
        Behaviors.same
      }
      case AdaptedResourcesStateResponse(resources) => {
        ctx.log.debug("Received AdaptedResourcesStateResponse")
        val senseResources = resources
          .filter(resource => resource.resourceType.contains(Sense))
          .filter(resource => resource.nodeState.get)
          .filter(resource => resource.state.nonEmpty)
        val actResources = resources
          .filter(resource => resource.resourceType.contains(Act))
          .filter(resource => resource.nodeState.get)
          .filter(resource => resource.state.nonEmpty)

        val sensorsForView: Map[String, Map[String, String]] = resources
          .filter(resource => resource.resourceType.contains(Sense))
          .map(resource => resource.name.getOrElse("") -> Map(
            "Status" -> resource.nodeState.map(state => if (state) "online" else "offline").getOrElse(""),
            "WaterLevel" -> resource.state.getOrElse("")))
          .toMap
        riverMonitorStateActor ! SensorsForView(sensorsForView)

        if senseResources.nonEmpty then
          if senseResources.count(resource =>
              resource.state.get.toFloat > riverMonitor.threshold
            ) > senseResources.size / 2
          then riverMonitorStateActor ! WarnRiverMonitor

        if actResources.nonEmpty then
          actResources.foreach(resource => {
            resource.state.get match
              case "Evacuating" => riverMonitorStateActor ! EvacuatingRiverMonitor
              case "Safe"       => riverMonitorStateActor ! EvacuatedRiverMonitor
          })
        Behaviors.same
      }
      case _ => {
        ctx.log.debug(s"Unexpected message. The actor is being stopped")
        Behaviors.stopped
      }
    }
