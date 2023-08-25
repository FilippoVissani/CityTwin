package it.unibo.citytwin.rivermonitor.actors.view

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
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
import it.unibo.citytwin.rivermonitor.view.View
import it.unibo.citytwin.core.model.{Resource, ResourceType}
import scala.concurrent.duration.DurationInt
import scala.util.Success

/** Command trait for messages that the ViewActor can receive.
  */
trait ViewActorCommand

/** Message received when "evacuate" button pressed
  */
object EvacuatingZone extends Serializable with ViewActorCommand

/** Message received when "evacuated" button pressed
  */
object EvacuatedZone extends Serializable with ViewActorCommand

/** A message representing a periodic tick event for the ViewActor. This is used to trigger the
  * ViewActor to perform periodic tasks.
  * @param resourcesToCheck
  *   A set of resource names to be checked during each tick.
  */
case class Tick(resourcesToCheck: Set[String]) extends Serializable with ViewActorCommand

/** Message received as a response when asking resources status
  * @param resources
  *   a set containing requested resources
  */
case class AdaptedResourcesStateResponse(resources: Set[Resource])
    extends Serializable
    with ViewActorCommand

/** An actor responsible for simulating a view behaviour.
  */
object ViewActor:
  /** Factory method to create a new ViewActor.
    *
    * @param viewName
    *   The name of the view.
    * @param resourcesToCheck
    *   A set of resource names to periodically check.
    * @param width
    *   The width of the view.
    * @param height
    *   The height of the view.
    * @return
    *   Behavior[ViewActorCommand]
    */
  def apply(
      viewName: String,
      resourcesToCheck: Set[String],
      width: Int,
      height: Int
  ): Behavior[ViewActorCommand] =
    Behaviors.setup[ViewActorCommand] { ctx =>
      val view: View    = View(width, height, viewName, ctx.self)
      val resourceActor = ctx.spawnAnonymous(ResourceActor())
      val resource = Resource(
        name = Some(viewName),
        state = Some("Safe"),
        resourceType = Set(ResourceType.Act)
      )
      resourceActor ! ResourceChanged(resource)
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick(resourcesToCheck), 1.seconds)
        viewActorLogic(ctx, view, viewName, resourceActor, resource)
      }
    }

  // Private method to define the behavior of the ViewActor
  private def viewActorLogic(
      ctx: ActorContext[ViewActorCommand],
      view: View,
      viewName: String,
      resourceActor: ActorRef[ResourceActorCommand],
      resource: Resource
  ): Behavior[ViewActorCommand] =
    implicit val timeout: Timeout = 3.seconds
    Behaviors.receiveMessage {
      case Tick(resourcesToCheck) => {
        ctx.log.debug("Received Tick")
        // Request resource status from the ResourceActor using AskResourcesToMainstay message
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
              view.updateRiverMonitorState(resource.state.get)
            })
        Behaviors.same
      }
      case EvacuatingZone => {
        ctx.log.debug("Received EvacuatingZone")
        // Send an "Evacuating" resource state to the ResourceActor
        resourceActor ! ResourceChanged(resource.copy(state = Some("Evacuating")))
        Behaviors.same
      }
      case EvacuatedZone => {
        ctx.log.debug("Received EvacuatedZone")
        // Send a "Safe" resource state to the ResourceActor
        resourceActor ! ResourceChanged(resource.copy(state = Some("Safe")))
        Behaviors.same
      }
      case _ => {
        ctx.log.debug(s"Unexpected message. The actor is being stopped")
        Behaviors.stopped
      }
    }
