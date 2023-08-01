package it.unibo.citytwin.core.actors

import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.Resource

import scala.collection.immutable.Set

trait MainstayActorCommand
case class AskResourceState(
    replyTo: ActorRef[ResourceActorCommand],
    name: String
) extends MainstayActorCommand
    with Serializable
case class UpdateResourceState(resource: Resource)
    extends MainstayActorCommand
    with Serializable
case class SetMainstayActors(refs: Set[ActorRef[MainstayActorCommand]])
    extends MainstayActorCommand
    with Serializable

object MainstayActor:
  val mainstayService: ServiceKey[MainstayActorCommand] =
    ServiceKey[MainstayActorCommand]("mainstayService")

  def apply(
      mainstays: Set[ActorRef[MainstayActorCommand]] = Set(),
      resources: Map[String, Resource] = Map()
  ): Behavior[MainstayActorCommand] =
    Behaviors.setup[MainstayActorCommand] { ctx =>
      Behaviors.receiveMessage {
        case AskResourceState(
              replyTo: ActorRef[ResourceActorCommand],
              name: String
            ) => {
          ctx.log.debug("AskResourceState")
          replyTo ! ResponseResourceState(resources.get(name))
          Behaviors.same
        }
        case UpdateResourceState(resource: Resource) => {
          ctx.log.debug("UpdateResourceState")
          mainstays.foreach(x => x ! UpdateResourceState(resource))
          MainstayActor(mainstays, resources + (resource.name -> resource))
        }
        case SetMainstayActors(refs: Set[ActorRef[MainstayActorCommand]]) => {
          ctx.log.debug("SetMainstayActors")
          MainstayActor(refs, resources)
        }
      }
    }
