package it.unibo.citytwin.core.actors

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.ServiceKey
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.Resource

trait ResourceActorCommand

/**
 * Message received by the `ResourceActor` from the `MainstayActor` as a response to a request for the current state of resources.
 *
 * @param resources A set of resources.
 */
case class AdaptedResourcesStateResponse(resources: Set[Resource]) extends ResourceActorCommand with Serializable

/**
 * Message received by the `ResourceActor` to save references to all Mainstay Actors with which it can communicate.
 *
 * @param mainstays A set containing references to all Mainstay Actors.
 */
case class SetMainstayActorsToResourceActor(mainstays: Set[ActorRef[MainstayActorCommand]]) extends ResourceActorCommand with Serializable

/**
 * Message received by the `ResourceActor` from the effective sensor or actuator actor to notify
 * that the resource has changed.
 *
 * @param resource The updated resource.
 */
case class ResourceChanged(resource: Resource) extends ResourceActorCommand with Serializable
/**
 * Used by view/actuators.
 * Message that contact the `MainstayActor` to ask for the current state of resources.
 *
 * @param names A set containing names of requested resources.
 */
case class AskResourcesToMainstay(names: Set[String]) extends ResourceActorCommand with Serializable

case class ResourcesFromMainstayResponse(resources: Set[Resource]) extends Serializable

object ResourceActor:
  val resourceService: ServiceKey[ResourceActorCommand] =
    ServiceKey[ResourceActorCommand]("resourceService")