package it.unibo.citytwin.rivermonitor.actors.rivermonitor

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.core.actors.ResourceActorCommand
import it.unibo.citytwin.rivermonitor.actors.floodsensor.FloodSensorActorCommand
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.*
import it.unibo.citytwin.rivermonitor.model.{RiverMonitor, Zone, ZoneState}

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
          ctx.log.debug("Received FreeRiverMonitor")
          RiverMonitorActor(riverMonitor.state_(Safe), resourceActor)
        }
        case EvacuatingRiverMonitor => {
          ctx.log.debug("Received BusyRiverMonitor")
          RiverMonitorActor(riverMonitor.state_(Evacuating), resourceActor)
        }
        case _ => {
          Behaviors.stopped
        }
      }
    }