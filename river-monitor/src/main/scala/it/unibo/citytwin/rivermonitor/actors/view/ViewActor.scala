package it.unibo.citytwin.rivermonitor.actors.view

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.rivermonitor.actors.*
import it.unibo.citytwin.rivermonitor.model.{FloodSensor, RiverMonitor, Zone}
import it.unibo.citytwin.rivermonitor.view.View

trait ViewActorCommand
case class UpdateFloodSensor(floodSensor: FloodSensor) extends Serializable with ViewActorCommand
case class UpdateZone(riverMonitor: RiverMonitor, zone: Zone) extends Serializable with ViewActorCommand
object EvacuatedZone extends Serializable with ViewActorCommand
object EvacuatingZone extends Serializable with ViewActorCommand
case class IsMyZoneResponseView(replyTo: ActorRef[RiverMonitorActorCommand]) extends Serializable with ViewActorCommand

val viewService = ServiceKey[ViewActorCommand]("viewService")

class ViewActor(ctx: ActorContext[ViewActorCommand],
                viewName: String,
                width: Int,
                height: Int) extends AbstractBehavior(ctx):

  val view: View = View(width, height, viewName, ctx.self)
  var riverMonitorActor: Option[ActorRef[RiverMonitorActorCommand]] = Option.empty
  
  override def onMessage(msg: ViewActorCommand): Behavior[ViewActorCommand] =
    msg match
      case UpdateFloodSensor(floodSensor) => {
        ctx.log.debug("Received UpdateFloodSensor")
        view.updateFloodSensor(floodSensor)
      }
      case UpdateZone(riverMonitor, zone) => {
        ctx.log.debug("Received UpdateZone")
        view.updateZone(zone)
        view.updateRiverMonitor(riverMonitor)
      }
      case EvacuatedZone => {
        ctx.log.debug("Received EvacuatedZone")
        if riverMonitorActor.isDefined then riverMonitorActor.get ! EvacuatedRiverMonitor
      }
      case EvacuatingZone => {
        ctx.log.debug("Received EvacuatingZone")
        if riverMonitorActor.isDefined then riverMonitorActor.get ! EvacuatingRiverMonitor
      }
      case IsMyZoneResponseView(replyTo) => {
        ctx.log.debug("Received IsMyZoneResponseView")
        riverMonitorActor = Option(replyTo)
      }
    this

