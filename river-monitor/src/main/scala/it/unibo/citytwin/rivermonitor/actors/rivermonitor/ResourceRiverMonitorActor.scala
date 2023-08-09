package it.unibo.citytwin.rivermonitor.actors.rivermonitor

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import it.unibo.citytwin.core.actors.ResourceActor.resourceService
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.model.Resource
import it.unibo.citytwin.core.model.ResourceType.{Act, Sense}
import scala.util.Success
import scala.concurrent.duration.DurationInt

object ResourceRiverMonitorActor :
  def apply(riverMonitorActor: ActorRef[RiverMonitorActorCommand],
            mainstayActors: Set[ActorRef[MainstayActorCommand]] = Set()): Behavior[ResourceActorCommand] =
    Behaviors.setup[ResourceActorCommand] { ctx =>
      Behaviors.withTimers { timers =>
        ctx.system.receptionist ! Receptionist.Register(resourceService, ctx.self)
        //TODO: take sensorsToCheck from riverMonitor (ask to riverMonitorActor)
        val sensorsToCheck = Set[String]("floodSensor1")
        timers.startTimerAtFixedRate(AskResourcesToMainstay(sensorsToCheck), 1.seconds)
        ResourceRiverMonitorActorLogic(ctx, riverMonitorActor, mainstayActors)
      }
    }

  def ResourceRiverMonitorActorLogic(ctx: ActorContext[ResourceActorCommand],
                                     riverMonitorActor: ActorRef[RiverMonitorActorCommand],
                                     mainstayActors: Set[ActorRef[MainstayActorCommand]]): Behavior[ResourceActorCommand] =
    implicit val timeout: Timeout = 3.seconds
    Behaviors.receiveMessage {
      case AskResourcesToMainstay(resourcesNames) => {
        ctx.log.debug("Received AskResourcesToMainstay")
        if mainstayActors.nonEmpty then
          ctx.ask(mainstayActors.head, ref => AskResourcesState(ref, resourcesNames)){
            case Success(ResourceStatesResponse(resources: Set[Resource])) => AdaptedResourcesStateResponse(resources)
            case _ => {
              ctx.log.debug("Resources not received from Mainstay actor. Mainstay actor is unreachable.")
              AdaptedResourcesStateResponse(Set())
            }
        }
        Behaviors.same
      }
      case AdaptedResourcesStateResponse(resources) => {
        ctx.log.debug("Received ResponseResourceState")
        val senseResources = resources.filter(resource => resource.resourceType.contains(Sense))
        val actResources = resources.filter(resource => resource.resourceType.contains(Act))

        if senseResources.nonEmpty then
          //se la maggioranza delle misurazioni è sopra la soglia metto in WARNING
          if senseResources.filter(resource => resource.state.nonEmpty).count(resource => resource.state.get.asInstanceOf[Float] > 5) > resources.size / 2 then
            riverMonitorActor ! WarnRiverMonitor

        if actResources.nonEmpty then
          ???

        Behaviors.same
      }
      case SetMainstayActorsToResourceActor(mainstayActors) => {
        ctx.log.debug("Received SetMainstayActorsToResourceActor")
        ResourceRiverMonitorActorLogic(ctx, riverMonitorActor, mainstayActors)
      }
      case ResourceChanged(resource) => {
        ctx.log.debug("Received ResourceChanged")
        //ogni volta che il riverMonitor passa da uno stato all'altro
        mainstayActors.foreach(mainstay => mainstay ! UpdateResources(Map(ctx.self -> resource).toSet))
        Behaviors.same
      }
      case _ => {
        ctx.log.debug(s"Unexpected message. The actor is being stopped")
        Behaviors.stopped
      }
    }

