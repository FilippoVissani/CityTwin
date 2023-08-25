package it.unibo.citytwin.core.actors

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.Resource
import scala.collection.immutable.{Map, Set}

/** MainstayActorCommand is the trait that defines the messages that can be sent to the
  * MainstayActor
  */
trait MainstayActorCommand

/** AskResourcesState is the message that can be sent to the MainstayActor to ask for the state of a
  * set of resources
  * @param replyTo
  *   the actor that will receive the response
  * @param names
  *   the names of the resources to ask for
  */
case class AskResourcesState(
    replyTo: ActorRef[ResourceStatesResponse],
    names: Set[String]
) extends MainstayActorCommand
    with Serializable

/** AskAllResourcesState is the message that can be sent to the MainstayActor to ask for the state
  * of all the resources
  * @param replyTo
  *   the actor that will receive the response
  */
case class AskAllResourcesState(
    replyTo: ActorRef[ResourceStatesResponse]
) extends MainstayActorCommand
    with Serializable

/** UpdateResources is the message that can be sent to the MainstayActor to update the state of a
  * set of resources
  * @param update
  *   the set of resources to update
  */
case class UpdateResources(
    update: Set[(ActorRef[ResourceActorCommand], Resource)]
) extends MainstayActorCommand
    with Serializable

/** SetMainstays is the message that can be sent to the MainstayActor to update the state of a set
  * of Mainstay Actors
  * @param nodes
  *   the set of Mainstay Actor's state
  */
case class SetMainstays(nodes: Set[(ActorRef[MainstayActorCommand], Boolean)])
    extends MainstayActorCommand
    with Serializable

/** Sync is the message that can be sent to the MainstayActor to synchronize the state of a set of
  * resources
  * @param update
  *   the set of resources to update
  */
case class Sync(update: Set[(ActorRef[ResourceActorCommand], Resource)])
    extends MainstayActorCommand
    with Serializable

/** ResourceStatesResponse is the message that is sent by the MainstayActor as a response to
  * AskResourcesState and AskAllResourcesState messages
  * @param resources
  *   the set of resources
  */
case class ResourceStatesResponse(resources: Set[Resource]) extends Serializable

/** MainstayActor is the actor that manages the state of the resources and the Mainstay Actors
  */
object MainstayActor:
  /** mainstayService is the key that identifies the Mainstay Actor in the Receptionist pattern
    */
  val mainstayService: ServiceKey[MainstayActorCommand] =
    ServiceKey[MainstayActorCommand]("mainstayService")

  /** Generates new Mainstay Actor.
    * @param persistenceServiceHost
    *   the host of the persistence service
    * @param persistenceServicePort
    *   the port of the persistence service
    * @return
    *   the behavior of Mainstay Actor.
    */
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
        result
          .filter((_, s) =>
            s.name.isDefined
              && s.nodeState.isDefined
              && s.resourceType.nonEmpty
          )
          .foreach((a, r) => persistenceServiceDriverActor ! PostResource(a.path.toString, r))
        mainstays
          .filter((m, _) => m != ctx.self)
          .filter((_, s) => s)
          .foreach((m, _) => m ! Sync(result.toSet))
        mainstayActorBehavior(ctx, persistenceServiceDriverActor, mainstays, result)
      }
      case SetMainstays(nodes: Set[(ActorRef[MainstayActorCommand], Boolean)]) => {
        ctx.log.debug("SetMainstays")
        nodes
          .filter((n, _) => n.path != ctx.self.path)
          .foreach((a, s) => persistenceServiceDriverActor ! PostMainstay(a.path.toString, s))
        mainstayActorBehavior(ctx, persistenceServiceDriverActor, nodes.toMap, resources)
      }
      case Sync(update: Set[(ActorRef[ResourceActorCommand], Resource)]) => {
        ctx.log.debug("Sync")
        mainstayActorBehavior(ctx, persistenceServiceDriverActor, mainstays, update.toMap)
      }
      case _ => {
        ctx.log.error("ERROR. Mainstay Actor stopped")
        Behaviors.stopped
      }
    }
