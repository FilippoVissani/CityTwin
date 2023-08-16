package it.unibo.citytwin.rivermonitor.actors.rivermonitor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.core.actors.{ResourceActorCommand, ResourceChanged}
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.*
import it.unibo.citytwin.rivermonitor.model.RiverMonitor
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.Resource
import it.unibo.citytwin.core.model.ResourceType.{Act, Sense}

trait RiverMonitorStateActorCommand

/**
 * Message to set the riverMonitor state as 'Warned'
 */
object WarnRiverMonitor extends Serializable with RiverMonitorStateActorCommand

/**
 * Message received from the view when clicking on "Evacuated" button.
 */
object EvacuatedRiverMonitor extends Serializable with RiverMonitorStateActorCommand

/**
 * Message received from the view when clicking on "Evacuate" button.
 */
object EvacuatingRiverMonitor extends Serializable with RiverMonitorStateActorCommand

object RiverMonitorStateActor:
  def apply(riverMonitor: RiverMonitor,
            resourceActor: ActorRef[ResourceActorCommand]): Behavior[RiverMonitorStateActorCommand] =
    Behaviors.setup[RiverMonitorStateActorCommand] { ctx =>
      val resource = Resource(Some(riverMonitor.riverMonitorName), Some(riverMonitor.position), Some(riverMonitor.state.toString), Set(Sense, Act))
      resourceActor ! ResourceChanged(resource)
      Behaviors.receiveMessage {
        case WarnRiverMonitor => {
          ctx.log.debug("Received WarnRiverMonitor")
          if riverMonitor.state == Safe then {
            RiverMonitorStateActor(riverMonitor.state_(Warned), resourceActor)
          }
          else Behaviors.same
        }
        case EvacuatedRiverMonitor => {
          ctx.log.debug("Received EvacuatedRiverMonitor")
          if riverMonitor.state == Evacuating then {
            RiverMonitorStateActor(riverMonitor.state_(Safe), resourceActor)
          }
          else Behaviors.same
        }
        case EvacuatingRiverMonitor => {
          ctx.log.debug("Received EvacuatingRiverMonitor")
          if riverMonitor.state == Warned then {
            RiverMonitorStateActor(riverMonitor.state_(Evacuating), resourceActor)
          }
          else Behaviors.same
        }
        case _ => {
          ctx.log.debug(s"Unexpected message. The actor is being stopped")
          Behaviors.stopped
        }
      }
    }