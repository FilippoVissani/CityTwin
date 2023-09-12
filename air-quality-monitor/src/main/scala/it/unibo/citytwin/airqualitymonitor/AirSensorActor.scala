package it.unibo.citytwin.airqualitymonitor

import akka.actor
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.util.ByteString
import akka.util.Timeout
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.model.ResourceState
import it.unibo.citytwin.core.model.ResourceType

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success

/** Commands supported by the AirSensorActor */
trait AirSensorActorCommand

/** A message representing a periodic tick event for the AirSensorActor. This object is used to
  * trigger the AirSensorActor to perform periodic tasks.
  */
object Tick extends Serializable with AirSensorActorCommand

/** An actor responsible for simulating an air sensor's behavior.
  */
object AirSensorActor:
  /** Create an instance of AirSensorActor.
    *
    * @param airSensor
    *   The air sensor instance.
    * @param sensorUri
    *   The URI for the sensor data.
    * @return
    *   Behavior[AirSensorActorCommand].
    */
  def apply(airSensor: AirSensor, sensorUri: String): Behavior[AirSensorActorCommand] =
    Behaviors.setup[AirSensorActorCommand] { ctx =>
      implicit val timeout: Timeout = 3.seconds
      val resourceActor             = ctx.spawnAnonymous(ResourceActor())
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick, 5.seconds)
        Behaviors.receiveMessage {
          case Tick => {
            ctx.log.info("Received Tick")
            implicit val executionContext: ExecutionContext = ctx.executionContext
            implicit val system: ActorSystem[Nothing]       = ctx.system
            val request                                     = HttpRequest(uri = sensorUri)
            val responseFuture: Future[HttpResponse]        = Http().singleRequest(request)
            responseFuture
              .onComplete {
                case Success(response) => {
                  response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
                    val resourceStateAsString: String = body.utf8String
                    val resource = ResourceState(
                      Some(airSensor.name),
                      Some(airSensor.position),
                      Some(resourceStateAsString),
                      Set(ResourceType.Sense)
                    )
                    resourceActor ! ResourceChanged(resource)
                  }
                }
                case Failure(ex) =>
              }
            Behaviors.same
          }
          case _ => {
            ctx.log.debug("Unexpected message. The actor is being stopped")
            Behaviors.stopped
          }
        }
      }
    }
