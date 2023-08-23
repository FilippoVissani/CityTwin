package it.unibo.citytwin.airqualitymonitor

import akka.actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.model.{Resource, ResourceType}
import it.unibo.citytwin.core.Serializable
import upickle.default.*
import upickle.default.{macroRW, ReadWriter as RW}
import upickle.*
import java.util.concurrent.ThreadLocalRandom
import scala.concurrent.duration.DurationInt

/** Commands supported by the AirSensorActor */
trait AirSensorActorCommand

/** A message representing a periodic tick event for the AirSensorActor. This object is used to
  * trigger the AirSensorActor to perform periodic tasks.
  */
object Tick extends Serializable with AirSensorActorCommand

object AirSensorActor:
  def apply(airSensor: AirSensor): Behavior[AirSensorActorCommand] =
    Behaviors.setup[AirSensorActorCommand] { ctx =>
      implicit val timeout: Timeout = 3.seconds
      val resourceActor             = ctx.spawnAnonymous(ResourceActor())
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick, 5.seconds)
        implicit val rw: RW[AirSensorResourceState] = macroRW
        Behaviors.receiveMessage {
          case Tick => {
            ctx.log.debug(s"Received Tick")
            // Calculate pm10, pm25 and nox. pm25 is a fraction of pm10
            val pm10 = ThreadLocalRandom.current().nextFloat(100)
            val pm25 = ThreadLocalRandom.current().nextFloat(pm10)
            val nox  = ThreadLocalRandom.current().nextFloat(150)
            // Create resource to send to mainstay
            val resourceStateAsString: String = write(AirSensorResourceState(pm10, pm25, nox))
            val resource = Resource(
              Some(airSensor.name),
              Some(airSensor.position),
              Some(resourceStateAsString),
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
