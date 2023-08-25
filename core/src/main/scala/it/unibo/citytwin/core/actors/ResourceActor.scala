package it.unibo.citytwin.core.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.util.Timeout
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.Resource
import concurrent.duration.DurationInt
import scala.util.{Random, Success}

/** ResourceActorCommand is the trait that defines the messages that can be sent to the
  * ResourceActor
  */
trait ResourceActorCommand

/** AdaptedResourcesStateResponse is the message that is sent by the ResourceActor as a response to
  * AskResourcesToMainstay and AskAllResourcesToMainstay messages
  *
  * @param replyTo
  *   the actor that will receive the response
  * @param resources
  *   the resources that are sent as a response
  */
case class AdaptedResourcesStateResponse(
    replyTo: ActorRef[ResourcesFromMainstayResponse],
    resources: Set[Resource]
) extends ResourceActorCommand
    with Serializable

/** SetMainstayActorsToResourceActor is the message that can be sent to the ResourceActor to set the
  * mainstay actors
  * @param mainstays
  *   the mainstay actors
  */
case class SetMainstayActorsToResourceActor(mainstays: Set[ActorRef[MainstayActorCommand]])
    extends ResourceActorCommand
    with Serializable

/** ResourceChanged is the message that can be sent to the ResourceActor to notify that a resource
  * has changed
  * @param resource
  *   the resource that has changed
  */
case class ResourceChanged(resource: Resource) extends ResourceActorCommand with Serializable

/** AskResourcesToMainstay is the message that can be sent to the ResourceActor to ask for a set of
  * resources to a mainstay actor
  *
  * @param replyTo
  *   the actor that will receive the response
  * @param names
  *   the names of the resources to ask
  */
case class AskResourcesToMainstay(
    replyTo: ActorRef[ResourcesFromMainstayResponse],
    names: Set[String]
) extends ResourceActorCommand
    with Serializable

/** AskAllResourcesToMainstay is the message that can be sent to the ResourceActor to ask for all
  * the resources to a mainstay actor
  *
  * @param replyTo
  *   the actor that will receive the response
  */
case class AskAllResourcesToMainstay(
    replyTo: ActorRef[ResourcesFromMainstayResponse]
) extends ResourceActorCommand
    with Serializable

/** AskMainstaysState is the message that can be sent to the ResourceActor to ask for the mainstay
  * actors
  * @param replyTo
  *   the actor that will receive the response
  */
case class AskMainstaysState(
    replyTo: ActorRef[MainstaysStateResponse]
) extends ResourceActorCommand
    with Serializable

/** ResourcesFromMainstayResponse is the message that is sent by the ResourceActor as a response to
  * AskResourcesToMainstay and AskAllResourcesToMainstay messages
  * @param resources
  *   the resources that are sent as a response
  */
case class ResourcesFromMainstayResponse(resources: Set[Resource]) extends Serializable

/** MainstaysStateResponse is the message that is sent by the ResourceActor as a response to
  * AskMainstaysState message
  * @param mainstays
  *   the mainstay actors
  */
case class MainstaysStateResponse(mainstays: Set[ActorRef[MainstayActorCommand]])
    extends Serializable

/** ResourceActor is the actor that manages the resource
  */
object ResourceActor:
  val resourceService: ServiceKey[ResourceActorCommand] =
    ServiceKey[ResourceActorCommand]("resourceService")

  /** Generates new ResourceActor.
    * @param mainstays
    *   the mainstay actors
    * @return
    *   the behavior of ResourceActor.
    */
  def apply(
      mainstays: Set[ActorRef[MainstayActorCommand]] = Set()
  ): Behavior[ResourceActorCommand] =
    Behaviors.setup[ResourceActorCommand] { ctx =>
      ctx.system.receptionist ! Receptionist.Register(resourceService, ctx.self)
      ctx.spawnAnonymous(ResourceGuardianActor(ctx.self))
      resourceActorBehaviour(ctx, mainstays)
    }

  private def resourceActorBehaviour(
      ctx: ActorContext[ResourceActorCommand],
      mainstays: Set[ActorRef[MainstayActorCommand]] = Set()
  ): Behavior[ResourceActorCommand] =
    implicit val timeout: Timeout = 3.seconds
    Behaviors.receiveMessage {
      case AdaptedResourcesStateResponse(
            replyTo: ActorRef[ResourcesFromMainstayResponse],
            resources: Set[Resource]
          ) => {
        replyTo ! ResourcesFromMainstayResponse(resources)
        Behaviors.same
      }
      case SetMainstayActorsToResourceActor(mainstays: Set[ActorRef[MainstayActorCommand]]) => {
        resourceActorBehaviour(ctx, mainstays)
      }
      case ResourceChanged(resource: Resource) => {
        mainstays.foreach(m => m ! UpdateResources(Set((ctx.self, resource))))
        Behaviors.same
      }
      case AskResourcesToMainstay(
            replyTo: ActorRef[ResourcesFromMainstayResponse],
            names: Set[String]
          ) => {
        if mainstays.nonEmpty then
          val selectedMainstay = Random.shuffle(mainstays).head
          ctx.ask(selectedMainstay, ref => AskResourcesState(ref, names)) {
            case Success(ResourceStatesResponse(resources: Set[Resource])) =>
              AdaptedResourcesStateResponse(replyTo, resources)
            case _ => AdaptedResourcesStateResponse(replyTo, Set())
          }
        Behaviors.same
      }
      case AskAllResourcesToMainstay(
            replyTo: ActorRef[ResourcesFromMainstayResponse]
          ) => {
        if mainstays.nonEmpty then
          val selectedMainstay = Random.shuffle(mainstays).head
          ctx.ask(selectedMainstay, ref => AskAllResourcesState(ref)) {
            case Success(ResourceStatesResponse(resources: Set[Resource])) =>
              AdaptedResourcesStateResponse(replyTo, resources)
            case _ => AdaptedResourcesStateResponse(replyTo, Set())
          }
        Behaviors.same
      }
      case AskMainstaysState(replyTo: ActorRef[MainstaysStateResponse]) => {
        replyTo ! MainstaysStateResponse(mainstays)
        Behaviors.same
      }
      case _ => {
        ctx.log.error("ERROR. Resource Actor stopped")
        Behaviors.stopped
      }
    }
