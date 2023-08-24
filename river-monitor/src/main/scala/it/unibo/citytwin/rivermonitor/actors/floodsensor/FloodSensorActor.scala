package it.unibo.citytwin.rivermonitor.actors.floodsensor

import akka.actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.model.{Resource, ResourceType}
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.rivermonitor.model.FloodSensor
import java.util.concurrent.ThreadLocalRandom
import scala.concurrent.duration.DurationInt

trait FloodSensorActorCommand

/** A message representing a periodic tick event for the FloodSensorActor. This object is used to
  * trigger the FloodSensorActor to perform periodic tasks.
  */
object Tick extends Serializable with FloodSensorActorCommand

/** An actor responsible for simulating a flood sensor's behavior.
  */
object FloodSensorActor:
  /** Factory method to create a new FloodSensorActor
    *
    * @param floodSensor
    *   The associated FloodSensor instance.
    * @return
    *   Behavior[FloodSensorActorCommand]
    */
  def apply(floodSensor: FloodSensor): Behavior[FloodSensorActorCommand] =
    Behaviors.setup[FloodSensorActorCommand] { ctx =>
      implicit val timeout: Timeout = 3.seconds
      val resourceActor             = ctx.spawnAnonymous(ResourceActor())
      // Set up timers for periodic Tick messages
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick, 5.seconds)
        // Message handling
        Behaviors.receiveMessage {
          case Tick => {
            ctx.log.debug(s"Received Tick")
            val waterLevel = ThreadLocalRandom.current().nextFloat(20)
            val resource = Resource(
              Some(floodSensor.name),
              Some(floodSensor.position),
              Some(waterLevel.toString),
              Set(ResourceType.Sense)
            )
            resourceActor ! ResourceChanged(resource)
            Behaviors.same
          }
          case _ => {
            ctx.log.debug(s"Unexpected message. The actor is being stopped")
            Behaviors.stopped
          }
        }
      }
    }
