package it.unibo.citytwin.control_panel.actors

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.DateTime
import akka.util.Timeout
import it.unibo.citytwin.control_panel.view.View
import it.unibo.citytwin.core.actors.AskAllResourcesToMainstay
import it.unibo.citytwin.core.actors.AskMainstaysHistory
import it.unibo.citytwin.core.actors.AskMainstaysState
import it.unibo.citytwin.core.actors.AskResourcesHistory
import it.unibo.citytwin.core.actors.AskResourcesToMainstay
import it.unibo.citytwin.core.actors.MainstayActorCommand
import it.unibo.citytwin.core.actors.MainstaysHistoryResponse
import it.unibo.citytwin.core.actors.MainstaysStateResponse
import it.unibo.citytwin.core.actors.PersistenceServiceDriverActor
import it.unibo.citytwin.core.actors.ResourceActor
import it.unibo.citytwin.core.actors.ResourceActorCommand
import it.unibo.citytwin.core.actors.ResourcesFromMainstayResponse
import it.unibo.citytwin.core.actors.ResourcesHistoryResponse
import it.unibo.citytwin.core.model.MainstayState
import it.unibo.citytwin.core.model.ResourceState

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import scala.util.Success

import concurrent.duration.DurationInt

/** ControlPanelActorCommand is the trait that defines the messages that can be sent to the
  * ControlPanelActor
  */
trait ControlPanelActorCommand

/** AdaptedResourcesFromMainstayResponse is the message that is sent by the ControlPanelActor as a
  * response to AskResourcesToMainstay and AskAllResourcesToMainstay messages
  *
  * @param resources
  *   the resources that are sent as a response
  */
case class AdaptedResourcesFromMainstayResponse(resources: Set[ResourceState])
    extends ControlPanelActorCommand
    with Serializable

/** AdaptedMainstaysStateResponse is the message that is sent by the ControlPanelActor as a response
  * to AskMainstaysState messages
  * @param mainstays
  *   the mainstays that are sent as a response
  */
case class AdaptedMainstaysStateResponse(mainstays: Set[ActorRef[MainstayActorCommand]])
    extends ControlPanelActorCommand
    with Serializable

/** AdaptedMainstaysHistoryResponse is the message that is sent by the ControlPanelActor as a
  * response to AskMainstaysHistory messages
  * @param states
  *   the states of the mainstays that are sent as a response
  */
case class AdaptedMainstaysHistoryResponse(states: Seq[MainstayState])
    extends ControlPanelActorCommand
    with Serializable

/** AdaptedResourcesHistoryResponse is the message that is sent by the ControlPanelActor as a
  * response to AskResourcesHistory messages
  * @param states
  *   the states of the resources that are sent as a response
  */
case class AdaptedResourcesHistoryResponse(states: Seq[ResourceState])
    extends ControlPanelActorCommand
    with Serializable

/** Tick is the message that is sent by the ControlPanelActor to itself to request the state of the
  * mainstays and the resources
  */
object Tick extends ControlPanelActorCommand with Serializable

/** ControlPanelActor is the actor that manages the control panel
  */
object ControlPanelActor:
  /** apply is the method that creates the behavior of the ControlPanelActor
    * @param citySize
    *   the size of the city
    * @return
    *   the behavior of the ControlPanelActor
    */
  def apply(
      citySize: (Double, Double),
      cityMap: String,
      persistenceServiceHost: String,
      persistenceServicePort: String
  ): Behavior[ControlPanelActorCommand] =
    Behaviors.setup[ControlPanelActorCommand] { ctx =>
      implicit val timeout: Timeout = 3.seconds
      val resourceActor             = ctx.spawnAnonymous(ResourceActor())
      val persistenceServiceDriverActor =
        ctx.spawnAnonymous(
          PersistenceServiceDriverActor(persistenceServiceHost, persistenceServicePort)
        )
      val view = View(citySize, cityMap)
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick, 8.seconds)
        Behaviors.receiveMessage {
          case AdaptedResourcesFromMainstayResponse(resources: Set[ResourceState]) =>
            ctx.log.debug(s"Received AdaptedResourcesFromMainstayResponse: $resources")
            view.drawResources(resources)
            Behaviors.same
          case AdaptedMainstaysStateResponse(mainstays: Set[ActorRef[MainstayActorCommand]]) =>
            ctx.log.debug(s"Received AdaptedMainstaysStateResponse: $mainstays")
            view.drawMainstays(mainstays.map(m => m.path.toString))
            Behaviors.same
          case AdaptedMainstaysHistoryResponse(states: Seq[MainstayState]) =>
            ctx.log.debug(s"Received AdaptedMainstayHistoryResponse $states")
            val statsData: Map[Timestamp, Int] =
              states
                .map(s => (s, Timestamp.valueOf(s.time.truncatedTo(ChronoUnit.MINUTES))))
                .filter((s, _) => s.state)
                .groupBy((_, t) => t)
                .map((k, v) => (k, v.map((s, _) => s.address).toSet.size))
            ctx.log.debug(s"Updating view with Mainstays: $statsData")
            view.drawMainstaysStats(statsData)
            Behaviors.same
          case AdaptedResourcesHistoryResponse(states: Seq[ResourceState]) =>
            ctx.log.debug(s"Received AdaptedResourcesHistoryResponse $states")
            val statsData: Map[Timestamp, Int] =
              states
                .filter(s => s.name.isDefined && s.nodeState.isDefined && s.time.isDefined)
                .map(s => (s, Timestamp.valueOf(s.time.get.truncatedTo(ChronoUnit.MINUTES))))
                .filter((s, _) => s.nodeState.get)
                .groupBy((_, t) => t)
                .map((k, v) => (k, v.map((s, _) => s.name.get).toSet.size))
            ctx.log.debug(s"Updating view with Resources: $statsData")
            view.drawResourcesStats(statsData)
            Behaviors.same
          case Tick =>
            ctx.ask(resourceActor, ref => AskMainstaysState(ref)) {
              case Success(
                    MainstaysStateResponse(mainstays: Set[ActorRef[MainstayActorCommand]])
                  ) =>
                AdaptedMainstaysStateResponse(mainstays)
              case _ => AdaptedMainstaysStateResponse(Set())
            }
            ctx.ask(resourceActor, ref => AskAllResourcesToMainstay(ref)) {
              case Success(ResourcesFromMainstayResponse(resources: Set[ResourceState])) =>
                AdaptedResourcesFromMainstayResponse(resources)
              case _ => AdaptedResourcesFromMainstayResponse(Set())
            }
            ctx.ask(persistenceServiceDriverActor, ref => AskMainstaysHistory(ref)) {
              case Success(MainstaysHistoryResponse(states: Seq[MainstayState])) =>
                AdaptedMainstaysHistoryResponse(states)
              case _ => AdaptedMainstaysHistoryResponse(Seq())
            }
            ctx.ask(persistenceServiceDriverActor, ref => AskResourcesHistory(ref)) {
              case Success(ResourcesHistoryResponse(states: Seq[ResourceState])) =>
                AdaptedResourcesHistoryResponse(states)
              case _ => AdaptedResourcesHistoryResponse(Seq())
            }
            Behaviors.same
        }
      }
    }
