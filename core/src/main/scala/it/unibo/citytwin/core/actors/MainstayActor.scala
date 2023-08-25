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

case class AskAllResourcesState(
    replyTo: ActorRef[ResourceStatesResponse]
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
      persistenceServiceHost: String,
      persistenceServicePort: String
  ): Behavior[MainstayActorCommand] =
    Behaviors.setup[MainstayActorCommand] { ctx =>
      ctx.log.debug("Mainstay started")
      ctx.system.receptionist ! Receptionist.Register(mainstayService, ctx.self)
      ctx.spawnAnonymous(NodesObserverGuardianActor(ctx.self))
      val persistenceServiceDriverActor = ctx.spawnAnonymous(
        PersistenceServiceDriverActor(persistenceServiceHost, persistenceServicePort)
      )
      mainstayActorBehavior(ctx, persistenceServiceDriverActor)
    }

  private def mainstayActorBehavior(
      ctx: ActorContext[MainstayActorCommand],
      persistenceServiceDriverActor: ActorRef[PersistenceServiceDriverActorCommand],
      mainstays: Map[ActorRef[MainstayActorCommand], Boolean] = Map(),
      resources: Map[ActorRef[ResourceActorCommand], Resource] = Map()
  ): Behavior[MainstayActorCommand] =
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
      case AskAllResourcesState(
            replyTo: ActorRef[ResourceStatesResponse]
          ) => {
        ctx.log.debug("AskAllResourcesState")
        replyTo ! ResourceStatesResponse(resources.values.toSet)
        Behaviors.same
      }
      case UpdateResources(update: Set[(ActorRef[ResourceActorCommand], Resource)]) => {
        ctx.log.debug(s"UpdateResources: $update")
        val updateMap = update.toMap
        val result: Map[ActorRef[ResourceActorCommand], Resource] = resources.map((k, v) =>
          if updateMap.contains(k) then (k, v.merge(updateMap(k))) else (k, v)
        ) ++ (updateMap -- resources.keys)
        result.filter((_, s) =>
          s.name.isDefined
          && s.nodeState.isDefined
          && s.state.isDefined
          && s.position.isDefined
          && s.resourceType.nonEmpty
        ).foreach((a, r) => persistenceServiceDriverActor ! PostResource(a.path.toString, r))
        mainstayActorBehavior(ctx, persistenceServiceDriverActor, mainstays, result)
      }
      case SetMainstays(nodes: Set[(ActorRef[MainstayActorCommand], Boolean)]) => {
        ctx.log.debug("SetMainstays")
        nodes.filter((n, _) => n.path != ctx.self.path).foreach((a, s) => persistenceServiceDriverActor ! PostMainstay(a.path.toString, s))
        mainstayActorBehavior(ctx, persistenceServiceDriverActor, nodes.toMap, resources)
      }
      case _ => {
        ctx.log.error("ERROR. Mainstay Actor stopped")
        Behaviors.stopped
      }
    }
