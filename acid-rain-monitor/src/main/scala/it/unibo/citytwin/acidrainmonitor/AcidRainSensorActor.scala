package it.unibo.citytwin.acidrainmonitor

import akka.actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.model.{Resource, ResourceType}
import it.unibo.citytwin.core.Serializable
import scala.concurrent.duration.DurationInt

/** Commands supported by the AcidRainSensorActor */
trait AcidRainSensorActorCommand

/** A message representing a periodic tick event for the AcidRainSensorActor. This object is used to
  * trigger the AcidRainSensorActor to perform periodic tasks.
  */
object Tick extends Serializable with AcidRainSensorActorCommand

object AcidRainSensorActor:
  def apply(acidRainSensor: AcidRainSensor): Behavior[AcidRainSensorActorCommand] =
    Behaviors.setup[AcidRainSensorActorCommand] { ctx =>
      implicit val timeout: Timeout = 3.seconds
      val resourceActor             = ctx.spawnAnonymous(ResourceActor())
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick, 5.seconds)
        Behaviors.receiveMessage {
          case Tick => {
            ctx.log.info(s"Received Tick")
            // Simulate pH measurement
            val pH = scala.util.Random.nextFloat() * 14 // pH range: 0-14
            // Create JSON string with pH value
            val json = s"""{"ph": $pH}"""
            // Create resource to send to mainstay
            val resource = Resource(
              Some(acidRainSensor.name),
              Some(acidRainSensor.position),
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
