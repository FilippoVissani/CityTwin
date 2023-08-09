package it.unibo.citytwin.core.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.Resource
import concurrent.duration.DurationInt
import scala.util.{Random, Success}

trait ResourceActorCommand

/** Message received by the `ResourceActor` from the `MainstayActor` as a response to a request for
  * the current state of resources.
  *
  * @param resources
  *   A set of resources.
  */
case class AdaptedResourcesStateResponse(
    replyTo: ActorRef[ResourcesFromMainstayResponse],
    resources: Set[Resource]
) extends ResourceActorCommand
    with Serializable

/** Message received by the `ResourceActor` to save references to all Mainstay Actors with which it
  * can communicate.
  *
  * @param mainstays
  *   A set containing references to all Mainstay Actors.
  */
case class SetMainstayActorsToResourceActor(mainstays: Set[ActorRef[MainstayActorCommand]])
    extends ResourceActorCommand
    with Serializable

/** Message received by the `ResourceActor` from the effective sensor or actuator actor to notify
  * that the resource has changed.
  *
  * @param resource
  *   The updated resource.
  */
case class ResourceChanged(resource: Resource) extends ResourceActorCommand with Serializable

/** Used by view/actuators. Message that contact the `MainstayActor` to ask for the current state of
  * resources.
  *
  * @param names
  *   A set containing names of requested resources.
  */
case class AskResourcesToMainstay(
    replyTo: ActorRef[ResourcesFromMainstayResponse],
    names: Set[String]
) extends ResourceActorCommand
    with Serializable

case class AskAllResourcesToMainstay(
    replyTo: ActorRef[ResourcesFromMainstayResponse]
) extends ResourceActorCommand
    with Serializable

case class ResourcesFromMainstayResponse(resources: Set[Resource]) extends Serializable

object ResourceActor:
  val resourceService: ServiceKey[ResourceActorCommand] =
    ServiceKey[ResourceActorCommand]("resourceService")

  def apply(
      mainstays: Set[ActorRef[MainstayActorCommand]] = Set()
  ): Behavior[ResourceActorCommand] =
    Behaviors.setup[ResourceActorCommand] { ctx =>
      ctx.system.receptionist ! Receptionist.Register(resourceService, ctx.self)
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
          ResourceActor(mainstays)
        }
        case ResourceChanged(resource: Resource) => {
          if mainstays.nonEmpty then
            val selectedMainstay = Random.shuffle(mainstays).head
            selectedMainstay ! UpdateResources(Set((ctx.self, resource)))
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
      }
    }
