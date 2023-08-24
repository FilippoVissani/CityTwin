package it.unibo.citytwin.noisepollutionmonitor

import akka.actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.model.{Resource, ResourceType}
import it.unibo.citytwin.core.Serializable
import scala.concurrent.duration.DurationInt

/** Commands supported by the NoiseSensorActor */
trait NoiseSensorActorCommand

/** A message representing a periodic tick event for the NoiseSensorActor. This object is used to
  * trigger the NoiseSensorActor to perform periodic tasks.
  */
object Tick extends Serializable with NoiseSensorActorCommand

/** An actor responsible for simulating a noise sensor's behavior.
  */
object NoiseSensorActor:
  /** Create an instance of NoiseSensorActor.
    *
    * @param noiseSensor
    *   The noise sensor instance.
    * @return
    *   Behavior[NoiseSensorActorCommand].
    */
  def apply(noiseSensor: NoiseSensor): Behavior[NoiseSensorActorCommand] =
    Behaviors.setup[NoiseSensorActorCommand] { ctx =>
      implicit val timeout: Timeout = 3.seconds
      val resourceActor             = ctx.spawnAnonymous(ResourceActor())
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick, 5.seconds)
        Behaviors.receiveMessage {
          case Tick => {
            ctx.log.info(s"Received Tick")
            // Simulate noise level measurement
            val noiseLevel = scala.util.Random.nextInt(61) + 40 // Noise level range: 40-100 dB
            // Create JSON string with noise level value and description
            val description = getNoiseDescription(noiseLevel)
            val json        = s"""{"value (dB)": $noiseLevel, "description": "$description"}"""
            // Create resource to send to mainstay
            val resource = Resource(
              Some(noiseSensor.name),
              Some(noiseSensor.position),
              Some(json),
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

  // Function to get noise description based on the noise level
  private def getNoiseDescription(noiseLevel: Int): String = noiseLevel match {
    case _ if noiseLevel <= 50 => "Quiet environment"
    case _ if noiseLevel <= 70 => "Moderate noise"
    case _ if noiseLevel <= 90 => "Noisy environment"
    case _                     => "Very noisy environment"
  }
