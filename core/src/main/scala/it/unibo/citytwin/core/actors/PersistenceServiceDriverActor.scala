package it.unibo.citytwin.core.actors

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Post
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.HttpResponse.unapply
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import it.unibo.citytwin.core.JSONDeSerializer.{jsonToMainstaysHistory, jsonToResourcesHistory, mainstayToJson, resourceToJson}
import it.unibo.citytwin.core.model.MainstayState
import it.unibo.citytwin.core.model.ResourceState
import play.api.libs.json.JsValue
import play.api.libs.json.Json

import java.time.LocalDate
import java.time.LocalDateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import concurrent.duration.DurationInt
import it.unibo.citytwin.core.PersistenceServiceAPISpec.*
import it.unibo.citytwin.core.PersistenceServiceAPISpec.Scheme.*
import it.unibo.citytwin.core.PersistenceServiceAPISpec.Resource.*

/** PersistenceServiceDriverActorCommand is the trait that defines the messages that can be sent to
  * the PersistenceServiceDriverActor
  */
trait PersistenceServiceDriverActorCommand

/** AskMainstaysHistory is the message that can be sent to the PersistenceServiceDriverActor to ask
  * for the history of the Mainstay Actors
  * @param replyTo
  *   the actor that will receive the response
  */
case class AskMainstaysHistory(replyTo: ActorRef[MainstaysHistoryResponse])
    extends PersistenceServiceDriverActorCommand
    with Serializable

/** AskResourcesHistory is the message that can be sent to the PersistenceServiceDriverActor to ask
  * for the history of the resources
  * @param replyTo
  *   the actor that will receive the response
  */
case class AskResourcesHistory(replyTo: ActorRef[ResourcesHistoryResponse])
    extends PersistenceServiceDriverActorCommand
    with Serializable
case class PostMainstay(state: MainstayState)
    extends PersistenceServiceDriverActorCommand
    with Serializable

/** PostResource is the message that can be sent to the PersistenceServiceDriverActor to send a
  * resource to the persistence service
  *
  * @param address
  *   the address of the resource
  * @param resource
  *   the resource to send
  */
case class PostResource(address: String, resource: ResourceState)
    extends PersistenceServiceDriverActorCommand
    with Serializable

/** MainstayHistoryResponse is the message that is sent by the PersistenceServiceDriverActor as a
  * response to AskMainstaysHistory message
  * @param states
  *   the history of the Mainstay Actors
  */
case class MainstaysHistoryResponse(states: Seq[MainstayState])

/** ResourcesHistoryResponse is the message that is sent by the PersistenceServiceDriverActor as a
  * response to AskResourcesHistory message
  * @param states
  *   the history of the resources
  */
case class ResourcesHistoryResponse(states: Seq[ResourceState])

/** PersistenceServiceDriverActor is the actor that manages the communication with the persistence
  * service
  */
object PersistenceServiceDriverActor:

  /** Generates new PersistenceServiceDriverActor.
    *
    * @param host
    *   the host of the persistence service
    * @param port
    *   the port of the persistence service
    * @return
    *   the behavior of PersistenceServiceDriverActor.
    */
  def apply(host: String, port: String): Behavior[PersistenceServiceDriverActorCommand] =
    Behaviors.setup[PersistenceServiceDriverActorCommand] { ctx =>
      ctx.log.debug("PersistenceServiceDriverActor started")
      implicit val executionContext: ExecutionContext = ctx.executionContext
      implicit val system: ActorSystem[Nothing]       = ctx.system
      Behaviors.receiveMessage {
        case AskMainstaysHistory(replyTo: ActorRef[MainstaysHistoryResponse]) =>
          ctx.log.debug("Received AskMainstayHistory")
          val response: Future[HttpResponse] =
            Http().singleRequest(HttpRequest(uri = generateURI(HTTP, host, port, Mainstays)))
          response
            .flatMap(resp => resp.entity.toStrict(1.seconds))
            .map(strictEntity => strictEntity.data.utf8String)
            .onComplete {
              case Success(result) =>
                replyTo ! MainstaysHistoryResponse(jsonToMainstaysHistory(result))
              case Failure(exception) => println(exception)
            }
          Behaviors.same
        case AskResourcesHistory(replyTo: ActorRef[ResourcesHistoryResponse]) =>
          ctx.log.debug("Received AskResourcesHistory")
          val response: Future[HttpResponse] =
            Http().singleRequest(HttpRequest(uri = generateURI(HTTP, host, port, Resources)))
          response
            .flatMap(resp => resp.entity.toStrict(1.seconds))
            .map(strictEntity => strictEntity.data.utf8String)
            .onComplete {
              case Success(result) =>
                replyTo ! ResourcesHistoryResponse(jsonToResourcesHistory(result))
              case Failure(exception) => println(exception)
            }
          Behaviors.same
        case PostMainstay(state: MainstayState) =>
          ctx.log.debug("Received PostMainstay")
          Http().singleRequest(
            HttpRequest(
              method = HttpMethods.POST,
              uri = generateURI(HTTP, host, port, Mainstays),
              entity = HttpEntity(ContentTypes.`application/json`, mainstayToJson(state))
            )
          )
          Behaviors.same
        case PostResource(address: String, resource: ResourceState) =>
          ctx.log.debug("Received PostResource")
          Http().singleRequest(
            HttpRequest(
              method = HttpMethods.POST,
              uri = generateURI(HTTP, host, port, Resources),
              entity = HttpEntity(ContentTypes.`application/json`, resourceToJson(address, resource))
            )
          )
          Behaviors.same
      }
    }
