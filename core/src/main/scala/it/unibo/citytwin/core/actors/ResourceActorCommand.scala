package it.unibo.citytwin.core.actors

import akka.actor.typed.receptionist.ServiceKey
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.Resource

trait ResourceActorCommand
case class AdaptedResourcesStateResponse(resources: Set[Resource]) extends ResourceActorCommand with Serializable

object ResourceActor:
  val resourceService: ServiceKey[ResourceActorCommand] =
    ServiceKey[ResourceActorCommand]("resourceService")
