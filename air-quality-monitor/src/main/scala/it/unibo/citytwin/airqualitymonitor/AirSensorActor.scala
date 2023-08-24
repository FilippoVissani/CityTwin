package it.unibo.citytwin.airqualitymonitor

import akka.actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.model.{Resource, ResourceType}
import it.unibo.citytwin.core.Serializable
import akka.util.ByteString
import scala.concurrent.duration.DurationInt
import akka.http.scaladsl.model.{HttpMethod, HttpRequest, HttpResponse}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import akka.http.scaladsl.Http

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
            ctx.log.info(s"Received Tick")
            implicit val executionContext: ExecutionContext = ctx.executionContext
            implicit val system: ActorSystem[Nothing]       = ctx.system
            val request                                     = HttpRequest(uri = sensorUri)
            val responseFuture: Future[HttpResponse]        = Http().singleRequest(request)
            responseFuture
              .onComplete {
                case Success(response) => {
                  response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
                    val resourceStateAsString: String = body.utf8String
                    val resource = Resource(
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
            ctx.log.debug(s"Unexpected message. The actor is being stopped")
            Behaviors.stopped
          }
        }
      }
    }
