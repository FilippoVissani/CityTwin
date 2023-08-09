package it.unibo.citytwin.rivermonitor.actors.rivermonitor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.core.actors.{ResourceActorCommand, ResourceChanged}
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.*
import it.unibo.citytwin.rivermonitor.model.RiverMonitor
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.Resource
import it.unibo.citytwin.core.model.ResourceType.{Act, Sense}

trait RiverMonitorActorCommand

/**
 * A message received by the RiverMonitorActor to set the reference to the ResourceActor.
 *
 * @param resourceActor The reference to the ResourceActor to communicate with.
 */
case class SetResourceActor(resourceActor: ActorRef[ResourceActorCommand]) extends Serializable with RiverMonitorActorCommand

/**
 * Message to set the riverMonitor state as 'Warned'
 */
object WarnRiverMonitor extends Serializable with RiverMonitorActorCommand

/**
 * Message received from the view when clicking on "Evacuated" button.
 */
object EvacuatedRiverMonitor extends Serializable with RiverMonitorActorCommand

/**
 * Message received from the view when clicking on "Evacuate" button.
 */
object EvacuatingRiverMonitor extends Serializable with RiverMonitorActorCommand

object RiverMonitorActor:
  def apply(riverMonitor: RiverMonitor,
            resourceActor: Option[ActorRef[ResourceActorCommand]] = Option.empty): Behavior[RiverMonitorActorCommand] =
    Behaviors.setup[RiverMonitorActorCommand] { ctx =>
      val resource = Resource(Some(riverMonitor.riverMonitorName), Some(riverMonitor.position), Some(riverMonitor.state), Set(Sense, Act))
      if (resourceActor.nonEmpty) resourceActor.get ! ResourceChanged(resource)
      Behaviors.receiveMessage {
        case SetResourceActor(resourceActor) => {
          ctx.log.debug(s"Received SetResourceActor")
          RiverMonitorActor(riverMonitor, Some(resourceActor))
        }
        case WarnRiverMonitor => {
          ctx.log.debug("Received WarnRiverMonitor")
          if riverMonitor.state == Safe then {
            RiverMonitorActor(riverMonitor.state_(Warned), resourceActor)
          }
          else Behaviors.same
        }
        case EvacuatedRiverMonitor => {
          ctx.log.debug("Received EvacuatedRiverMonitor")
          if riverMonitor.state == Evacuating then {
            RiverMonitorActor(riverMonitor.state_(Safe), resourceActor)
          }
          else Behaviors.same
        }
        case EvacuatingRiverMonitor => {
          ctx.log.debug("Received EvacuatingRiverMonitor")
          if riverMonitor.state == Warned then {
            RiverMonitorActor(riverMonitor.state_(Evacuating), resourceActor)
          }
          else Behaviors.same
        }
        case _ => {
          ctx.log.debug(s"Unexpected message. The actor is being stopped")
          Behaviors.stopped
        }
      }
    }