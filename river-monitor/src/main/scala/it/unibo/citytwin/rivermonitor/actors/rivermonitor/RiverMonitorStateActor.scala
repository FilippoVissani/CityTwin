package it.unibo.citytwin.rivermonitor.actors.rivermonitor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.core.actors.{ResourceActorCommand, ResourceChanged}
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.*
import it.unibo.citytwin.rivermonitor.model.RiverMonitor
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.Resource
import it.unibo.citytwin.core.model.ResourceType.{Act, Sense}
import upickle.default._
import upickle.default.{ReadWriter => RW, macroRW}
import upickle._

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

case class SensorsForView(sensorsForView: Map[String, Map[String, String]])
    extends Serializable
    with RiverMonitorStateActorCommand

case class RiverMonitorResourceState(
    riverMonitorState: String,
    threshold: Float,
    sensorsForView: Option[Map[String, Map[String, String]]]
)

object RiverMonitorStateActor:
  def apply(
      riverMonitor: RiverMonitor,
      resourceActor: ActorRef[ResourceActorCommand],
      sensorsForView: Option[Map[String, Map[String, String]]] = None
  ): Behavior[RiverMonitorStateActorCommand] =
    Behaviors.setup[RiverMonitorStateActorCommand] { ctx =>

      val riverMonitorResourceState = RiverMonitorResourceState(
        riverMonitor.state.toString,
        riverMonitor.threshold,
        sensorsForView
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
            RiverMonitorStateActor(riverMonitor.copy(state = Warned), resourceActor, sensorsForView)
          } else Behaviors.same
        }
        case EvacuatedRiverMonitor => {
          ctx.log.debug("Received EvacuatedRiverMonitor")
          if riverMonitor.state == Evacuating then {
            RiverMonitorStateActor(riverMonitor.copy(state = Safe), resourceActor, sensorsForView)
          } else Behaviors.same
        }
        case EvacuatingRiverMonitor => {
          ctx.log.debug("Received EvacuatingRiverMonitor")
          if riverMonitor.state == Warned then {
            RiverMonitorStateActor(
              riverMonitor.copy(state = Evacuating),
              resourceActor,
              sensorsForView
            )
          } else Behaviors.same
        }
        case SensorsForView(sensorsForView) => {
          ctx.log.debug("Received SensorsForView")
          RiverMonitorStateActor(riverMonitor, resourceActor, Some(sensorsForView))
        }
        case _ => {
          ctx.log.debug(s"Unexpected message. The actor is being stopped")
          Behaviors.stopped
        }
      }
    }
