package it.unibo.citytwin.rivermonitor.actors.rivermonitor

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import it.unibo.citytwin.core.actors.ResourceActor.resourceService
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.model.Resource
import it.unibo.citytwin.rivermonitor.actors.rivermonitor.RiverMonitorActorCommand
import scala.util.{Failure, Success}
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
        ctx.ask(mainstayActors.head, ref => AskResourcesState(ref, resourcesNames)){
              //TODO: check success case received message
          case Success(ResponseResourceState(resources)) => ResponseResourceState(resources)
          case _ => {
            ctx.log.debug("Resources not received from Mainstay actor. Mainstay actor is unreachable.")
            ResponseResourceState(Set())
          }
        }
        Behaviors.same
      }
      case ResponseResourceState(resources) => {
        ctx.log.debug("Received ResponseResourceState")
        elaborateResources(resources)
        Behaviors.same
      }
      case SetMainstayActorsToResourceActor(mainstayActors) => {
        ctx.log.debug("Received SetMainstayActorsToResourceActor")
        ResourceRiverMonitorActor(riverMonitorActor, mainstayActors)
      }
      case ResourceChanged(resource) => {
        ctx.log.debug("Received ResourceChanged")
        mainstayActors.head ! SetResourceState(ctx.self, Some(resource))
        Behaviors.same
      }
      case _ => {
        ctx.log.debug(s"Unexpected message. The actor is being stopped")
        Behaviors.stopped
      }
    }

  def elaborateResources(resources: Set[Resource]): Unit =
    ???

