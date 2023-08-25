package it.unibo.citytwin.control_panel.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.DateTime
import akka.util.Timeout
import it.unibo.citytwin.control_panel.view.View
import it.unibo.citytwin.core.actors.{AskAllResourcesToMainstay, AskMainstaysHistory, AskMainstaysState, AskResourcesHistory, AskResourcesToMainstay, MainstayActorCommand, MainstaysHistoryResponse, MainstaysStateResponse, PersistenceServiceDriverActor, ResourceActor, ResourceActorCommand, ResourcesFromMainstayResponse, ResourcesHistoryResponse}
import it.unibo.citytwin.core.model.{MainstayState, Resource}
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

case class AdaptedMainstaysHistoryResponse(states: Seq[(MainstayState, LocalDateTime)])
    extends ControlPanelActorCommand
    with Serializable

case class AdaptedResourcesHistoryResponse(states: Seq[(Resource, LocalDateTime)])
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
          case AdaptedMainstaysHistoryResponse(states: Seq[(MainstayState, LocalDateTime)]) => {
            ctx.log.debug(s"Received AdaptedMainstayHistoryResponse $states")
            val statsData: Map[Timestamp, Int] =
              states
                .map((s, t) => (s, Timestamp.valueOf(t.truncatedTo(ChronoUnit.MINUTES))))
                .filter((s, _) => s.address.isDefined && s.state.isDefined)
                .filter((s, _) => s.state.get)
                .groupBy((_, t) => t)
                .map((k, v) => (k, v.map((s, _) => s.address.get).toSet.size))
            ctx.log.debug(s"Updating view with Mainstays: $statsData")
            view.drawMainstaysStats(statsData)
            Behaviors.same
          }
          case AdaptedResourcesHistoryResponse(states: Seq[(Resource, LocalDateTime)]) => {
            ctx.log.debug(s"Received AdaptedResourcesHistoryResponse $states")
            val statsData: Map[Timestamp, Int] =
              states
                .map((s, t) => (s, Timestamp.valueOf(t.truncatedTo(ChronoUnit.MINUTES))))
                .filter((s, _) => s.name.isDefined && s.nodeState.isDefined)
                .filter((s, _) => s.nodeState.get)
                .groupBy((_, t) => t)
                .map((k, v) => (k, v.map((s, _) => s.name.get).toSet.size))
            ctx.log.debug(s"Updating view with Resources: $statsData")
            view.drawResourcesStats(statsData)
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
              case Success(MainstaysHistoryResponse(states: Seq[(MainstayState, LocalDateTime)])) =>
                AdaptedMainstaysHistoryResponse(states)
              case _ => AdaptedMainstaysHistoryResponse(Seq())
            }
            ctx.ask(persistenceServiceDriverActor, ref => AskResourcesHistory(ref)) {
              case Success(ResourcesHistoryResponse(states: Seq[(Resource, LocalDateTime)])) =>
                AdaptedResourcesHistoryResponse(states)
              case _ => AdaptedResourcesHistoryResponse(Seq())
            }
            Behaviors.same
          }
        }
      }
    }
