package it.unibo.citytwin.rivermonitor.actors.view

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import it.unibo.citytwin.core.actors.{
  AskResourcesToMainstay,
  ResourceActor,
  ResourceActorCommand,
  ResourceChanged,
  ResourcesFromMainstayResponse
}
import it.unibo.citytwin.rivermonitor.actors.*
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.{Evacuating, RiverMonitorState, Safe}
import it.unibo.citytwin.rivermonitor.model.{FloodSensor, RiverMonitor}
import it.unibo.citytwin.rivermonitor.view.View
import it.unibo.citytwin.core.model.{Resource, ResourceType}

import scala.concurrent.duration.DurationInt
import scala.util.Success

trait ViewActorCommand

/** Message received when "evacuate" button pressed
  */
object EvacuatingZone extends Serializable with ViewActorCommand

/** Message received when "evacuated" button pressed
  */
object EvacuatedZone extends Serializable with ViewActorCommand

/** A message representing a periodic tick event for the ViewActor. This is used to trigger the
  * ViewActor to perform periodic tasks.
  */
case class Tick(resourcesToCheck: Set[String]) extends Serializable with ViewActorCommand

/** Message received as a response when asking resources status
  * @param resources
  *   a set containing requested resources
  */
case class AdaptedResourcesStateResponse(resources: Set[Resource])
    extends Serializable
    with ViewActorCommand

object ViewActor:
  def apply(
      viewName: String,
      resourcesToCheck: Set[String],
      width: Int,
      height: Int
  ): Behavior[ViewActorCommand] =
    Behaviors.setup[ViewActorCommand] { ctx =>
      val view: View    = View(width, height, viewName, ctx.self)
      val resourceActor = ctx.spawnAnonymous(ResourceActor())
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick(resourcesToCheck), 1.seconds)
        viewActorLogic(ctx, view, viewName, resourceActor)
      }
    }

  private def viewActorLogic(
      ctx: ActorContext[ViewActorCommand],
      view: View,
      viewName: String,
      resourceActor: ActorRef[ResourceActorCommand]
  ): Behavior[ViewActorCommand] =
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
        if resources.nonEmpty then
          resources
            .filter(resource => resource.state.nonEmpty)
            .foreach(resource => {
              view.updateRiverMonitorState(resource.state.get.asInstanceOf[String])
            })
        Behaviors.same
      }
      case EvacuatingZone => {
        ctx.log.debug("Received EvacuatingZone")
        val resource = Resource(
          name = Some(viewName),
          state = Some("Evacuating"),
          resourceType = Set(ResourceType.Act)
        )
        resourceActor ! ResourceChanged(resource)
        Behaviors.same
      }
      case EvacuatedZone => {
        ctx.log.debug("Received EvacuatedZone")
        val resource = Resource(
          name = Some(viewName),
          state = Some("Safe"),
          resourceType = Set(ResourceType.Act)
        )
        resourceActor ! ResourceChanged(resource)
        Behaviors.same
      }
      case _ => {
        ctx.log.debug(s"Unexpected message. The actor is being stopped")
        Behaviors.stopped
      }
    }
