package it.unibo.citytwin.core.actors

import akka.actor.typed.receptionist.ServiceKey
import it.unibo.citytwin.core.model.Resource

trait ResourceActorCommand
case class ResponseResourceState(resources: Set[Resource])
    extends ResourceActorCommand

object ResourceActor:
  val resourceService: ServiceKey[ResourceActorCommand] =
    ServiceKey[ResourceActorCommand]("resourceService")