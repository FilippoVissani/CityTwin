package it.unibo.citytwin.control_panel.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import it.unibo.citytwin.control_panel.view.View
import it.unibo.citytwin.core.actors.{
  AskAllResourcesToMainstay,
  AskMainstaysState,
  AskResourcesToMainstay,
  MainstayActorCommand,
  MainstaysStateResponse,
  ResourceActor,
  ResourceActorCommand,
  ResourcesFromMainstayResponse
}
import it.unibo.citytwin.core.model.Resource

import concurrent.duration.DurationInt
import scala.util.Success

trait ControlPanelActorCommand
case class AdaptedResourcesFromMainstayResponse(resources: Set[Resource])
    extends ControlPanelActorCommand
    with Serializable

case class AdaptedMainstaysStateResponse(mainstays: Set[ActorRef[MainstayActorCommand]])
    extends ControlPanelActorCommand
    with Serializable
object Tick extends ControlPanelActorCommand with Serializable
object ControlPanelActor:
  def apply(citySize: (Double, Double)): Behavior[ControlPanelActorCommand] =
    Behaviors.setup[ControlPanelActorCommand] { ctx =>
      implicit val timeout: Timeout = 3.seconds
      val resourceActor             = ctx.spawnAnonymous(ResourceActor())
      val view                      = View(citySize)
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick, 5.seconds)
        Behaviors.receiveMessage {
          case AdaptedResourcesFromMainstayResponse(resources: Set[Resource]) => {
            ctx.log.debug(s"Received resources: ${resources}")
            view.drawResources(resources)
            Behaviors.same
          }
          case AdaptedMainstaysStateResponse(mainstays: Set[ActorRef[MainstayActorCommand]]) => {
            ctx.log.debug(s"Received mainstays: ${mainstays}")
            view.drawMainstays(mainstays.map(m => m.path.toString))
            Behaviors.same
          }
          case Tick => {
            ctx.ask(resourceActor, ref => AskMainstaysState(ref)) {
              case Success(
                    MainstaysStateResponse(mainstays: Set[ActorRef[MainstayActorCommand]])
                  ) =>
                AdaptedMainstaysStateResponse(mainstays)
              case _ => AdaptedMainstaysStateResponse(Set())
            }
            ctx.ask(resourceActor, ref => AskAllResourcesToMainstay(ref)) {
              case Success(ResourcesFromMainstayResponse(resources: Set[Resource])) =>
                AdaptedResourcesFromMainstayResponse(resources)
              case _ => AdaptedResourcesFromMainstayResponse(Set())
            }
            Behaviors.same
          }
        }
      }
    }
