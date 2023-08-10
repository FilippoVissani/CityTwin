package it.unibo.citytwin.rivermonitor.actors.view

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.core.actors.{ResourceActorCommand, ResourceChanged}
import it.unibo.citytwin.rivermonitor.actors.*
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.{Evacuating, RiverMonitorState, Safe}
import it.unibo.citytwin.rivermonitor.model.{FloodSensor, RiverMonitor}
import it.unibo.citytwin.rivermonitor.view.View
import it.unibo.citytwin.core.model.{Resource, ResourceType}

trait ViewActorCommand
case class UpdateRiverMonitorState(riverMonitorState: String) extends Serializable with ViewActorCommand
object EvacuatingZone extends Serializable with ViewActorCommand
object EvacuatedZone extends Serializable with ViewActorCommand

/**
 * A message received by the ViewActor to set the reference to the ResourceActor.
 *
 * @param resourceActor The reference to the ResourceActor to communicate with.
 */
case class SetResourceActor(resourceActor: ActorRef[ResourceActorCommand]) extends Serializable with ViewActorCommand


object ViewActor :
  def apply(viewName: String, width: Int, height: Int): Behavior[ViewActorCommand] =
    Behaviors.setup[ViewActorCommand] { ctx =>
      val view: View = View(width, height, viewName, ctx.self)
      viewActorLogic(ctx, view, viewName, width, height)
    }

  private def viewActorLogic(ctx: ActorContext[ViewActorCommand], view:View, viewName: String, width: Int, height: Int,
                             resourceActor: Option[ActorRef[ResourceActorCommand]] = Option.empty): Behavior[ViewActorCommand] =
    Behaviors.receiveMessage {
      case SetResourceActor(resourceActor) => {
        ctx.log.debug(s"Received SetResourceActor")
        viewActorLogic(ctx, view, viewName, width, height, Some(resourceActor))
      }
      case UpdateRiverMonitorState(riverMonitorState: String) => {
        ctx.log.debug("Received UpdateRiverMonitorState")
        view.updateRiverMonitorState(riverMonitorState)
        Behaviors.same
      }
      case EvacuatingZone => {
          ctx.log.debug("Received EvacuatingZone")
          //messaggio ricevuto dalla pressione del bottone evacuate
          //mandare al resourceActor la resource che indichi questo
          val resource = Resource(name = Some(viewName), state = Some("Evacuating"), resourceType = Set(ResourceType.Act))
          if resourceActor.isDefined then resourceActor.get ! ResourceChanged(resource)
          Behaviors.same
      }
      case EvacuatedZone => {
        ctx.log.debug("Received EvacuatedZone")
        //messaggio ricevuto dalla pressione del bottone evacuated
        //mandare al resourceActor la resource che indichi questo
        val resource = Resource(name = Some(viewName), state = Some("Safe"), resourceType = Set(ResourceType.Act))
        if resourceActor.isDefined then resourceActor.get ! ResourceChanged(resource)
        Behaviors.same
      }
    }