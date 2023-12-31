package it.unibo.citytwin.core.actors

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.{MainstayState, ResourceState}

import java.time.LocalDateTime
import scala.collection.immutable.Map
import scala.collection.immutable.Set

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
    update: Set[(ActorRef[ResourceActorCommand], ResourceState)]
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
case class Sync(update: Set[(ActorRef[ResourceActorCommand], ResourceState)])
    extends MainstayActorCommand
    with Serializable

/** ResourceStatesResponse is the message that is sent by the MainstayActor as a response to
  * AskResourcesState and AskAllResourcesState messages
  * @param resources
  *   the set of resources
  */
case class ResourceStatesResponse(resources: Set[ResourceState]) extends Serializable

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
      resources: Map[ActorRef[ResourceActorCommand], ResourceState] = Map()
  ): Behavior[MainstayActorCommand] =
    Behaviors.receiveMessage {
      case AskResourcesState(
            replyTo: ActorRef[ResourceStatesResponse],
            names: Set[String]
          ) =>
        ctx.log.debug("AskResourceState")
        replyTo ! ResourceStatesResponse(
          resources.values
            .filter(x => x.name.isDefined)
            .filter(x => names.contains(x.name.get))
            .toSet
        )
        Behaviors.same
      case AskAllResourcesState(
            replyTo: ActorRef[ResourceStatesResponse]
          ) =>
        ctx.log.debug("AskAllResourcesState")
        replyTo ! ResourceStatesResponse(resources.values.toSet)
        Behaviors.same
      case UpdateResources(update: Set[(ActorRef[ResourceActorCommand], ResourceState)]) =>
        ctx.log.debug(s"UpdateResources: $update")
        // merge the update with current states
        val mergedUpdate = update
          .map((a, r) => (a, r.copy(time = Some(LocalDateTime.now()))))
          .map((a, r) => if resources.contains(a) then (a, resources(a).merge(r)) else (a, r))
        // send it to persistence service
        mergedUpdate
          .filter((_, r) => r.name.isDefined && r.nodeState.isDefined && r.resourceType.nonEmpty)
          .foreach((a, r) => persistenceServiceDriverActor ! PostResource(a.path.toString, r))
        // compute the overall result
        val result = resources ++ mergedUpdate
        // sync with other mainstays
        mainstays
          .filter((m, _) => m != ctx.self)
          .filter((_, s) => s)
          .foreach((m, _) => m ! Sync(result.toSet))
        // start new behavior
        mainstayActorBehavior(
          ctx,
          persistenceServiceDriverActor,
          mainstays,
          result
        )
      case SetMainstays(nodes: Set[(ActorRef[MainstayActorCommand], Boolean)]) =>
        ctx.log.debug("SetMainstays")
        nodes
          .filter((n, _) => n.path != ctx.self.path)
          .foreach((a, s) =>
            persistenceServiceDriverActor ! PostMainstay(
              MainstayState(a.toString, s, LocalDateTime.now())
            )
          )
        mainstayActorBehavior(ctx, persistenceServiceDriverActor, nodes.toMap, resources)
      case Sync(update: Set[(ActorRef[ResourceActorCommand], ResourceState)]) =>
        ctx.log.debug("Sync")
        mainstayActorBehavior(
          ctx,
          persistenceServiceDriverActor,
          mainstays,
          mergeResourcesUpdate(resources, update.toMap)
        )
      case _ =>
        ctx.log.error("ERROR. Mainstay Actor stopped")
        Behaviors.stopped
    }
  end mainstayActorBehavior

  private def mergeResourcesUpdate(
      actual: Map[ActorRef[ResourceActorCommand], ResourceState],
      update: Map[ActorRef[ResourceActorCommand], ResourceState]
  ): Map[ActorRef[ResourceActorCommand], ResourceState] =
    actual.map((k, v) =>
      if update.contains(k) then (k, v.merge(update(k))) else (k, v)
    ) ++ (update -- actual.keys)
