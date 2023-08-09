package it.unibo.citytwin.core.actors

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.Resource

import scala.collection.immutable.{Map, Set}

trait MainstayActorCommand
case class AskResourcesState(
    replyTo: ActorRef[ResourceStatesResponse],
    names: Set[String]
) extends MainstayActorCommand
    with Serializable
case class UpdateResources(
    update: Set[(ActorRef[ResourceActorCommand], Resource)]
) extends MainstayActorCommand
    with Serializable
case class SetMainstays(nodes: Set[(ActorRef[MainstayActorCommand], Boolean)])
    extends MainstayActorCommand
    with Serializable

case class ResourceStatesResponse(resources: Set[Resource]) extends Serializable

object MainstayActor:
  val mainstayService: ServiceKey[MainstayActorCommand] =
    ServiceKey[MainstayActorCommand]("mainstayService")

  def apply(
      mainstays: Map[ActorRef[MainstayActorCommand], Boolean] = Map(),
      resources: Map[ActorRef[ResourceActorCommand], Resource] = Map(),
      isNodesObserverGuardianStarted: Boolean = false
  ): Behavior[MainstayActorCommand] =
    Behaviors.setup[MainstayActorCommand] { ctx =>
      ctx.log.debug("Mainstay started")
      ctx.system.receptionist ! Receptionist.Register(mainstayService, ctx.self)
      if !isNodesObserverGuardianStarted then
        ctx.spawnAnonymous(NodesObserverGuardianActor(ctx.self))
      Behaviors.receiveMessage {
        case AskResourcesState(
              replyTo: ActorRef[ResourceStatesResponse],
              names: Set[String]
            ) => {
          ctx.log.debug("AskResourceState")
          replyTo ! ResourceStatesResponse(
            resources.values
              .filter(x => x.name.isDefined)
              .filter(x => names.contains(x.name.get))
              .toSet
          )
          Behaviors.same
        }
        case UpdateResources(update: Set[(ActorRef[ResourceActorCommand], Resource)]) => {
          ctx.log.debug("UpdateResources")
          val updateMap = update.toMap
          mainstays
            .filter((k, _) => k != ctx.self)
            .foreach((k, v) => if v then k ! UpdateResources(update))
          val result: Map[ActorRef[ResourceActorCommand], Resource] = resources.map((k, v) =>
            if updateMap.contains(k) then (k, v.merge(updateMap(k))) else (k, v)
          ) ++ (updateMap -- resources.keys)
          MainstayActor(mainstays, result, true)
        }
        case SetMainstays(nodes: Set[(ActorRef[MainstayActorCommand], Boolean)]) => {
          ctx.log.debug("SetMainstays")
          MainstayActor(nodes.toMap, resources, true)
        }
      }
    }
