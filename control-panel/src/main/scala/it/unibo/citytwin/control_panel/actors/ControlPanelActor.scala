package it.unibo.citytwin.control_panel.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import it.unibo.citytwin.core.actors.{AskAllResourcesToMainstay, AskResourcesToMainstay, ResourceActor, ResourceActorCommand, ResourcesFromMainstayResponse}
import it.unibo.citytwin.core.model.Resource

import concurrent.duration.DurationInt
import scala.util.Success

trait ControlPanelActorCommand
case class AdaptedResourcesFromMainstayResponse(resources: Set[Resource]) extends ControlPanelActorCommand with Serializable
object Tick extends ControlPanelActorCommand with Serializable
object ControlPanelActor:
  def apply(resourceActor: Option[ActorRef[ResourceActorCommand]] = None): Behavior[ControlPanelActorCommand] =
    Behaviors.setup[ControlPanelActorCommand] { ctx =>
      implicit val timeout: Timeout = 3.seconds
      if resourceActor.isEmpty then
        ControlPanelActor(Some(ctx.spawnAnonymous(ResourceActor())))
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick, 5.seconds)
        Behaviors.receiveMessage {
          case AdaptedResourcesFromMainstayResponse(resources: Set[Resource]) => ???
          case Tick => {
            ctx.ask(resourceActor.get, ref => AskAllResourcesToMainstay(ref)) {
              case Success(ResourcesFromMainstayResponse(resources: Set[Resource])) => AdaptedResourcesFromMainstayResponse(resources)
              case _ => AdaptedResourcesFromMainstayResponse(Set())
            }
            Behaviors.same
          }
        }
      }
    }
