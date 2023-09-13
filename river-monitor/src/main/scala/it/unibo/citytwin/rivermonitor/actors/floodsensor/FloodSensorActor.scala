package it.unibo.citytwin.rivermonitor.actors.floodsensor

import akka.actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.model.ResourceState
import it.unibo.citytwin.core.model.ResourceType
import it.unibo.citytwin.rivermonitor.model.FloodSensor
import upickle._
import upickle.default._
import java.util.concurrent.ThreadLocalRandom
import scala.concurrent.duration.DurationInt
import it.unibo.citytwin.rivermonitor.model.FloodSensorData

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
            ctx.log.debug("Received Tick")
            val waterLevel      = ThreadLocalRandom.current().nextFloat(20)
            val floodSensorData = FloodSensorData(waterLevel)
            // serialize waterLevel as json string
            val json: String = write(floodSensorData)
            // Create a ResourceState instance to represent the state
            val resource = ResourceState(
              Some(floodSensor.name),
              Some(floodSensor.position),
              Some(json),
              Set(ResourceType.Sense)
            )
            resourceActor ! ResourceChanged(resource)
            Behaviors.same
          }
          case _ => {
            ctx.log.debug("Unexpected message. The actor is being stopped")
            Behaviors.stopped
          }
        }
      }
    }
