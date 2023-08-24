package it.unibo.citytwin.control_panel.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.DateTime
import akka.util.Timeout
import it.unibo.citytwin.control_panel.view.View
import it.unibo.citytwin.core.actors.{
  AskAllResourcesToMainstay,
  AskMainstaysHistory,
  AskMainstaysState,
  AskResourcesHistory,
  AskResourcesToMainstay,
  MainstayActorCommand,
  MainstaysHistoryResponse,
  MainstaysStateResponse,
  PersistenceServiceDriverActor,
  ResourceActor,
  ResourceActorCommand,
  ResourcesFromMainstayResponse,
  ResourcesHistoryResponse
}
import it.unibo.citytwin.core.model.Resource

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import concurrent.duration.DurationInt
import scala.util.Success

trait ControlPanelActorCommand
case class AdaptedResourcesFromMainstayResponse(resources: Set[Resource])
    extends ControlPanelActorCommand
    with Serializable

case class AdaptedMainstaysStateResponse(mainstays: Set[ActorRef[MainstayActorCommand]])
    extends ControlPanelActorCommand
    with Serializable

case class AdaptedMainstaysHistoryResponse(states: Seq[(Boolean, LocalDateTime)])
    extends ControlPanelActorCommand
    with Serializable

case class AdaptedResourcesHistoryResponse(states: Seq[(Boolean, LocalDateTime)])
    extends ControlPanelActorCommand
    with Serializable

object Tick extends ControlPanelActorCommand with Serializable
object ControlPanelActor:
  def apply(citySize: (Double, Double)): Behavior[ControlPanelActorCommand] =
    Behaviors.setup[ControlPanelActorCommand] { ctx =>
      implicit val timeout: Timeout = 3.seconds
      val resourceActor             = ctx.spawnAnonymous(ResourceActor())
      val persistenceServiceDriverActor =
        ctx.spawnAnonymous(PersistenceServiceDriverActor("127.0.0.1", "8080"))
      val view = View(citySize)
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick, 8.seconds)
        Behaviors.receiveMessage {
          case AdaptedResourcesFromMainstayResponse(resources: Set[Resource]) => {
            ctx.log.debug(s"Received AdaptedResourcesFromMainstayResponse: ${resources}")
            view.drawResources(resources)
            Behaviors.same
          }
          case AdaptedMainstaysStateResponse(mainstays: Set[ActorRef[MainstayActorCommand]]) => {
            ctx.log.debug(s"Received AdaptedMainstaysStateResponse: $mainstays")
            view.drawMainstays(mainstays.map(m => m.path.toString))
            Behaviors.same
          }
          case AdaptedMainstaysHistoryResponse(states: Seq[(Boolean, LocalDateTime)]) => {
            ctx.log.debug(s"Received AdaptedMainstayHistoryResponse $states")
            val data: Map[Timestamp, Int] = states
              .filter((s, _) => s)
              .map((_, t) => Timestamp.valueOf(t.truncatedTo(ChronoUnit.SECONDS)))
              .groupBy(t => t)
              .map((k, v) => (k, v.length))
            ctx.log.debug(s"Updating view with: $data")
            view.drawMainstaysStats(data)
            Behaviors.same
          }
          case AdaptedResourcesHistoryResponse(states: Seq[(Boolean, LocalDateTime)]) => {
            ctx.log.debug(s"Received AdaptedResourcesHistoryResponse $states")
            val data: Map[Timestamp, Int] = states
              .filter((s, _) => s)
              .map((_, t) => Timestamp.valueOf(t.truncatedTo(ChronoUnit.SECONDS)))
              .groupBy(t => t)
              .map((k, v) => (k, v.length))
            ctx.log.debug(s"Updating view with: $data")
            view.drawResourcesStats(data)
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
            ctx.ask(persistenceServiceDriverActor, ref => AskMainstaysHistory(ref)) {
              case Success(MainstaysHistoryResponse(states: Seq[(Boolean, LocalDateTime)])) =>
                AdaptedMainstaysHistoryResponse(states)
              case _ => AdaptedMainstaysHistoryResponse(Seq())
            }
            ctx.ask(persistenceServiceDriverActor, ref => AskResourcesHistory(ref)) {
              case Success(ResourcesHistoryResponse(states: Seq[(Boolean, LocalDateTime)])) =>
                AdaptedResourcesHistoryResponse(states)
              case _ => AdaptedResourcesHistoryResponse(Seq())
            }
            Behaviors.same
          }
        }
      }
    }
