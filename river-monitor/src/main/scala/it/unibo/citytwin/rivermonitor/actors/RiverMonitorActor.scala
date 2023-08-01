package it.unibo.citytwin.rivermonitor.actors
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.*
import it.unibo.citytwin.rivermonitor.model.{RiverMonitor, Zone, ZoneState}
trait RiverMonitorActorCommand
object WarnRiverMonitor extends Serializable with RiverMonitorActorCommand
object EvacuatedRiverMonitor extends Serializable with RiverMonitorActorCommand
object EvacuatingRiverMonitor extends Serializable with RiverMonitorActorCommand
case class IsMyZoneRequestFromViewToRiverMonitor(zoneId: Int, replyTo: ActorRef[ViewActorCommand]) extends Serializable with RiverMonitorActorCommand
case class IsMyZoneRequestFromFloodSensorToRiverMonitor(zoneId: Int, replyTo: ActorRef[FloodSensorActorCommand]) extends Serializable with RiverMonitorActorCommand
case class SetViews(views: Set[ActorRef[ViewActorCommand]]) extends Serializable with RiverMonitorActorCommand

val riverMonitorService = ServiceKey[RiverMonitorActorCommand]("riverMonitorService")

object RiverMonitorActor:
  def apply(riverMonitor: RiverMonitor,
            zone: Zone,
            viewActors: Set[ActorRef[ViewActorCommand]] = Set()): Behavior[RiverMonitorActorCommand] =
    viewActors.foreach(viewActor => viewActor ! UpdateZone(riverMonitor, zone))
    Behaviors.setup[RiverMonitorActorCommand] { ctx =>
      ctx.system.receptionist ! Receptionist.Register(riverMonitorService, ctx.self)
      Behaviors.receiveMessage {
        case SetViews(views) => {
          ctx.log.debug("Received SetViews")
          RiverMonitorActor(riverMonitor, zone, views)
        }
        case WarnRiverMonitor => {
          ctx.log.debug("Received WarnRiverMonitor")
          if riverMonitor.state == Free then {
            RiverMonitorActor(riverMonitor.state_(Warned), zone.state_(ZoneState.Alarm), viewActors)
          }
          else Behaviors.same
        }
        case EvacuatedRiverMonitor => {
          ctx.log.debug("Received FreeRiverMonitor")
          RiverMonitorActor(riverMonitor.state_(Free), zone.state_(ZoneState.Ok), viewActors)
        }
        case EvacuatingRiverMonitor => {
          ctx.log.debug("Received BusyRiverMonitor")
          RiverMonitorActor(riverMonitor.state_(Evacuating), zone.state_(ZoneState.UnderManagement), viewActors)
        }
        case IsMyZoneRequestFromViewToRiverMonitor(zoneId, replyTo) => {
          ctx.log.debug("Received IsMyZoneRequestFromViewToRiverMonitor")
          if riverMonitor.zoneId == zoneId then
            replyTo ! IsMyZoneResponseView(ctx.self)
          Behaviors.same
        }
        case IsMyZoneRequestFromFloodSensorToRiverMonitor(zoneId, replyTo) => {
          ctx.log.debug("Received IsMyZoneRequestFromFloodSensorToRiverMonitor")
          if riverMonitor.zoneId == zoneId then
            replyTo ! IsMyZoneResponseFromRiverMonitorToFloodSensor(ctx.self)
          Behaviors.same
        }
        case _ => {
          Behaviors.stopped
        }
      }
    }