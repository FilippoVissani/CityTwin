package it.unibo.citytwin.acidrainmonitor

import akka.actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.model.ResourceState
import it.unibo.citytwin.core.model.ResourceType
import upickle._
import upickle.default._
import scala.concurrent.duration.DurationInt

/** Commands supported by the AcidRainSensorActor */
trait AcidRainSensorActorCommand

/** A message representing a periodic tick event for the AcidRainSensorActor. This object is used to
  * trigger the AcidRainSensorActor to perform periodic tasks.
  */
object Tick extends Serializable with AcidRainSensorActorCommand

/** An actor responsible for simulating an acid rain sensor's behavior.
  */
object AcidRainSensorActor:
  /** Creates a new instance of AcidRainSensorActor.
    *
    * @param acidRainSensor
    *   The AcidRainSensor object representing the sensor's properties.
    * @return
    *   Behavior[AcidRainSensorActorCommand].
    */
  def apply(acidRainSensor: AcidRainSensor): Behavior[AcidRainSensorActorCommand] =
    Behaviors.setup[AcidRainSensorActorCommand] { ctx =>
      implicit val timeout: Timeout = 3.seconds
      val resourceActor             = ctx.spawnAnonymous(ResourceActor())
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick, 5.seconds)
        Behaviors.receiveMessage {
          case Tick => {
            ctx.log.info("Received Tick")
            // Simulate pH measurement
            val pH = scala.util.Random.nextFloat() * 14 // pH range: 0-14
            // serialize pH value as JSON string
            val acidRainSensorData = AcidRainSensorData(pH)
            val json: String       = write(acidRainSensorData)
            // Create resource to send to mainstay
            val resource = ResourceState(
              Some(acidRainSensor.name),
              Some(acidRainSensor.position),
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
