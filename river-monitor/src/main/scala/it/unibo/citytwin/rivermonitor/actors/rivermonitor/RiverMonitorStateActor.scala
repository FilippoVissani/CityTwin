package it.unibo.citytwin.rivermonitor.actors.rivermonitor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.actors.ResourceActorCommand
import it.unibo.citytwin.core.actors.ResourceChanged
import it.unibo.citytwin.core.model.ResourceState
import it.unibo.citytwin.core.model.ResourceType.Act
import it.unibo.citytwin.core.model.ResourceType.Sense
import it.unibo.citytwin.rivermonitor.model.RiverMonitor
import it.unibo.citytwin.rivermonitor.model.RiverMonitorData
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.*
import upickle.*
import upickle.default.*
import upickle.default.macroRW
import upickle.default.{ReadWriter => RW}

/** Command trait for messages that the RiverMonitorStateActor can receive.
  */
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

/** An actor responsible for simulating a river monitor state.
  */
object RiverMonitorStateActor:
  /** Factory method to create a new RiverMonitorStateActor.
    *
    * @param riverMonitor
    *   The RiverMonitor instance associated with the actor.
    * @param resourceActor
    *   The ActorRef of the ResourceActor.
    * @param monitoredSensors
    *   Optional monitored sensor data.
    * @return
    *   Behavior[RiverMonitorStateActorCommand]
    */
  def apply(
      riverMonitor: RiverMonitor,
      resourceActor: ActorRef[ResourceActorCommand],
      monitoredSensors: Option[Map[String, Map[String, String]]] = None
  ): Behavior[RiverMonitorStateActorCommand] =
    Behaviors.setup[RiverMonitorStateActorCommand] { ctx =>
      // Serialize the data in JSON
      val riverMonitorData = RiverMonitorData(
        riverMonitor.state,
        riverMonitor.threshold,
        monitoredSensors
      )
      implicit val rw: RW[RiverMonitorData] = macroRW
      val json: String                      = write(riverMonitorData)

      // Create a Resource instance to represent the river monitor's state
      val resource = ResourceState(
        Some(riverMonitor.riverMonitorName),
        Some(riverMonitor.position),
        Some(json),
        Set(Sense, Act)
      )
      resourceActor ! ResourceChanged(resource)

      // Define the actor's behavior based on received messages
      Behaviors.receiveMessage {
        case WarnRiverMonitor => {
          ctx.log.debug("Received WarnRiverMonitor")
          if riverMonitor.state == Safe then {
            RiverMonitorStateActor(
              riverMonitor.copy(state = Warned),
              resourceActor,
              monitoredSensors
            )
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
          ctx.log.debug("Unexpected message. The actor is being stopped")
          Behaviors.stopped
        }
      }
    }
