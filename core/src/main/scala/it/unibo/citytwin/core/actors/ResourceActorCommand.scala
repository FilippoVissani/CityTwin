package it.unibo.citytwin.core.actors

import it.unibo.citytwin.core.model.Resource

trait ResourceActorCommand
case class ResponseResourceState(resource: Option[Resource])
    extends ResourceActorCommand
