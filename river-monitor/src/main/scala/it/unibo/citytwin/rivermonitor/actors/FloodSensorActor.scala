package it.unibo.citytwin.rivermonitor.actors
import akka.actor
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import it.unibo.citytwin.rivermonitor.model.FloodSensor
import akka.util.Timeout
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import it.unibo.citytwin.core.actors.ResourceActor.resourceService
import it.unibo.citytwin.core.actors.{MainstayActorCommand, ResourceActorCommand, ResourceChanged, ResponseResourceState, SetResourceState}
import it.unibo.citytwin.core.model.Resource
import it.unibo.citytwin.core.model.ResourceType

import scala.util.{Failure, Success}
import concurrent.duration.DurationInt
import java.util.concurrent.ThreadLocalRandom

trait FloodSensorActorCommand

/**
 * A message representing a periodic tick event for the FloodSensorActor.
 * This object is used to trigger the FloodSensorActor to perform periodic tasks.
 */
object Tick extends Serializable with FloodSensorActorCommand
/**
 * A message received by the FloodSensorActor to set the reference to the ResourceActor.
 *
 * @param resourceActor The reference to the ResourceActor to communicate with.
 */
case class SetResourceActor(resourceActor: ActorRef[ResourceActorCommand]) extends Serializable with FloodSensorActorCommand

object FloodSensorActor :
  def apply(floodSensor: FloodSensor): Behavior[FloodSensorActorCommand] =
    Behaviors.setup[FloodSensorActorCommand] { ctx =>
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick, 5.seconds)
        FloodSensorActorLogic(ctx, floodSensor, Option.empty)
      }
    }

  def FloodSensorActorLogic(ctx: ActorContext[FloodSensorActorCommand],
                            floodSensor: FloodSensor,
                            resourceActor: Option[ActorRef[ResourceActorCommand]]): Behavior[FloodSensorActorCommand] =
    implicit val timeout: Timeout = 2.seconds
    Behaviors.receiveMessage {
      case Tick => {
        ctx.log.debug(s"Received Tick")
        val waterLevel = ThreadLocalRandom.current().nextFloat(20)
        val resource = Resource(floodSensor.name, Some(floodSensor.position), Some(waterLevel), Set(ResourceType.Sense))
        if (resourceActor.nonEmpty) resourceActor.get ! ResourceChanged(resource)
        Behaviors.same
      }
      case SetResourceActor(resourceActor) => {
        ctx.log.debug(s"Received SetResourceActor")
        FloodSensorActorLogic(ctx, floodSensor, Some(resourceActor))
      }
      case _ => {
        ctx.log.debug(s"Unexpected message. The actor is being stopped")
        Behaviors.stopped
      }
    }