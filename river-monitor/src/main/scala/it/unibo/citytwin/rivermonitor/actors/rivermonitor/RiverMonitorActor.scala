package it.unibo.citytwin.rivermonitor.actors.rivermonitor

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import scala.util.Success
import concurrent.duration.DurationInt
import it.unibo.citytwin.core.actors.{AskResourcesToMainstay, ResourceActor, ResourceActorCommand, ResourceChanged, ResourcesFromMainstayResponse}
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.*
import it.unibo.citytwin.rivermonitor.model.RiverMonitor
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.Resource
import it.unibo.citytwin.core.model.ResourceType.{Act, Sense}

trait RiverMonitorActorCommand

/**
 * A message representing a periodic tick event for the RiverMonitorActor.
 * This is used to trigger the RiverMonitorActor to perform periodic tasks.
 */
case class Tick(resourcesToCheck: Set[String]) extends Serializable with RiverMonitorActorCommand

case class AdaptedResourcesStateResponse(resources: Set[Resource]) extends Serializable with RiverMonitorActorCommand

object RiverMonitorActor:
  def apply(riverMonitor: RiverMonitor,
            resourcesToCheck: Set[String]): Behavior[RiverMonitorActorCommand] =
    Behaviors.setup[RiverMonitorActorCommand] { ctx =>
      implicit val timeout: Timeout = 3.seconds
      val resourceActor = ctx.spawnAnonymous(ResourceActor())
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick(resourcesToCheck), 1.seconds)
        RiverMonitorActorLogic(ctx, riverMonitor, resourceActor)
      }
    }

  private def RiverMonitorActorLogic(ctx: ActorContext[RiverMonitorActorCommand],
                                     riverMonitor: RiverMonitor,
                                     resourceActor: ActorRef[ResourceActorCommand]): Behavior[RiverMonitorActorCommand] =
    implicit val timeout: Timeout = 3.seconds
    val resource = Resource(Some(riverMonitor.riverMonitorName), Some(riverMonitor.position), Some(riverMonitor.state.toString), Set(Sense, Act))
    resourceActor ! ResourceChanged(resource)
    //TODO: controllare che non lo esegua anche con il Behaviors.same ma solo quando chiami la funzione RiverMonitorActorLogic
    Behaviors.receiveMessage {
      case Tick(resourcesToCheck) => {
        ctx.log.debug("Received Tick")
        ctx.ask(resourceActor, ref => AskResourcesToMainstay(ref, resourcesToCheck)) {
          case Success(ResourcesFromMainstayResponse(resources: Set[Resource])) => AdaptedResourcesStateResponse(resources)
          case _ => {
            ctx.log.debug("Resources not received. Actor is unreachable.")
            AdaptedResourcesStateResponse(Set())
          }
        }
        Behaviors.same
      }
      case AdaptedResourcesStateResponse(resources) => {
        ctx.log.debug("Received AdaptedResourcesStateResponse")
        val senseResources = resources.filter(resource => resource.resourceType.contains(Sense)).filter(resource => resource.state.nonEmpty)
        val actResources = resources.filter(resource => resource.resourceType.contains(Act)).filter(resource => resource.state.nonEmpty)

        if senseResources.nonEmpty then
          if senseResources.count(resource => resource.state.get.asInstanceOf[Float] > 5) > senseResources.size / 2 then
            WarnRiverMonitor(ctx, riverMonitor, resourceActor)

        if actResources.nonEmpty then
          actResources.foreach(resource => {
            resource.state.get.asInstanceOf[String] match
              case "Evacuating" => EvacuatingRiverMonitor(ctx, riverMonitor, resourceActor)
              case "Safe" => EvacuatedRiverMonitor(ctx, riverMonitor, resourceActor)
          })
        Behaviors.same
      }
      case _ => {
        ctx.log.debug(s"Unexpected message. The actor is being stopped")
        Behaviors.stopped
      }
    }

  private def WarnRiverMonitor(ctx: ActorContext [RiverMonitorActorCommand], riverMonitor: RiverMonitor, resourceActor: ActorRef[ResourceActorCommand]): Unit =
    if riverMonitor.state == Safe then
      RiverMonitorActorLogic(ctx, riverMonitor.state_(Warned), resourceActor)

  private def EvacuatingRiverMonitor(ctx: ActorContext[RiverMonitorActorCommand], riverMonitor: RiverMonitor, resourceActor: ActorRef[ResourceActorCommand]): Unit =
    if riverMonitor.state == Warned then
      RiverMonitorActorLogic(ctx, riverMonitor.state_(Evacuating), resourceActor)

  private def EvacuatedRiverMonitor(ctx: ActorContext[RiverMonitorActorCommand], riverMonitor: RiverMonitor, resourceActor: ActorRef[ResourceActorCommand]): Unit =
    if riverMonitor.state == Evacuating then
      RiverMonitorActorLogic(ctx, riverMonitor.state_(Safe), resourceActor)