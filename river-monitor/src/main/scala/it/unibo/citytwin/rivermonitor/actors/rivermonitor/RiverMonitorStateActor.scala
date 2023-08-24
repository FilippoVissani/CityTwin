package it.unibo.citytwin.rivermonitor.actors.rivermonitor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.core.actors.{ResourceActorCommand, ResourceChanged}
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.*
import it.unibo.citytwin.rivermonitor.model.{RiverMonitor, RiverMonitorResourceState}
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.Resource
import it.unibo.citytwin.core.model.ResourceType.{Act, Sense}
import upickle.default.*
import upickle.default.{macroRW, ReadWriter as RW}
import upickle.*

trait RiverMonitorStateActorCommand

/** Message to set the riverMonitor state as 'Warned'
  */
object WarnRiverMonitor extends Serializable with RiverMonitorStateActorCommand

/** Message received from the view when clicking on "Evacuated" button.
  */
object EvacuatedRiverMonitor extends Serializable with RiverMonitorStateActorCommand

/** Message received from the view when clicking on "Evacuate" button.
  */
object EvacuatingRiverMonitor extends Serializable with RiverMonitorStateActorCommand

/** Message received by the RiverMonitorActor when he receives sensors from the Mainstay
  *
  * @param monitoredSensors
  *   a map containing monitored sensors
  */
case class MonitoredSensors(monitoredSensors: Map[String, Map[String, String]])
    extends Serializable
    with RiverMonitorStateActorCommand

object RiverMonitorStateActor:
  def apply(
      riverMonitor: RiverMonitor,
      resourceActor: ActorRef[ResourceActorCommand],
      monitoredSensors: Option[Map[String, Map[String, String]]] = None
  ): Behavior[RiverMonitorStateActorCommand] =
    Behaviors.setup[RiverMonitorStateActorCommand] { ctx =>

      val riverMonitorResourceState = RiverMonitorResourceState(
        riverMonitor.state.toString,
        riverMonitor.threshold,
        monitoredSensors
      )
      implicit val rw: RW[RiverMonitorResourceState] = macroRW
      val resourceStateAsString: String              = write(riverMonitorResourceState)

      val resource = Resource(
        Some(riverMonitor.riverMonitorName),
        Some(riverMonitor.position),
        Some(resourceStateAsString),
        Set(Sense, Act)
      )
      resourceActor ! ResourceChanged(resource)
      Behaviors.receiveMessage {
        case WarnRiverMonitor => {
          ctx.log.debug("Received WarnRiverMonitor")
          if riverMonitor.state == Safe then {
            RiverMonitorStateActor(riverMonitor.copy(state = Warned), resourceActor, monitoredSensors)
          } else Behaviors.same
        }
        case EvacuatedRiverMonitor => {
          ctx.log.debug("Received EvacuatedRiverMonitor")
          if riverMonitor.state == Evacuating then {
            RiverMonitorStateActor(riverMonitor.copy(state = Safe), resourceActor, monitoredSensors)
          } else Behaviors.same
        }
        case EvacuatingRiverMonitor => {
          ctx.log.debug("Received EvacuatingRiverMonitor")
          if riverMonitor.state == Warned then {
            RiverMonitorStateActor(
              riverMonitor.copy(state = Evacuating),
              resourceActor,
              monitoredSensors
            )
          } else Behaviors.same
        }
        case MonitoredSensors(monitoredSensors) => {
          ctx.log.debug("Received monitoredSensors")
          RiverMonitorStateActor(riverMonitor, resourceActor, Some(monitoredSensors))
        }
        case _ => {
          ctx.log.debug(s"Unexpected message. The actor is being stopped")
          Behaviors.stopped
        }
      }
    }
