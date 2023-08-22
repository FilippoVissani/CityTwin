package it.unibo.citytwin.core.actors

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Post
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethod, HttpMethods, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import it.unibo.citytwin.core.model.Resource
import play.api.libs.json.Json
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait PersistenceServiceDriverActorCommand

case class PostMainstay(address: String, state: Boolean) extends PersistenceServiceDriverActorCommand
  with Serializable

case class PostResource(address: String, resource: Resource) extends PersistenceServiceDriverActorCommand
  with Serializable

object PersistenceServiceDriverActor:

  def apply(host: String, port: String): Behavior[PersistenceServiceDriverActorCommand] =
    Behaviors.setup[PersistenceServiceDriverActorCommand] { ctx =>
      ctx.log.debug("PersistenceServiceDriverActor started")
      implicit val executionContext: ExecutionContext = ctx.executionContext
      implicit val system: ActorSystem[Nothing] = ctx.system
      Behaviors.receiveMessage {
        case PostMainstay(address: String, state: Boolean) => {
          ctx.log.debug("Received PostMainstay")
          val body = Json.obj(
            "address" -> address,
            "state" -> state,
          ).toString()
          Http().singleRequest(HttpRequest(
            method = HttpMethods.POST,
            uri = s"http://$host:$port/mainstay",
            entity = HttpEntity(ContentTypes.`application/json`, body)
          ))
          Behaviors.same
        }
        case PostResource(address: String, resource: Resource) => {
          ctx.log.debug("Received PostResource")
          var body = Json.obj(
            "address" -> address,
            "name" -> resource.name.orNull,
            "state" -> resource.state.orNull,
            "resource_type" -> resource.resourceType,
          )
          if resource.position.isDefined then
            body = body ++ Json.obj(
              "position_x" -> resource.position.get.x,
              "position_y" -> resource.position.get.y,
            )
          if resource.nodeState.isDefined then
            body = body ++ Json.obj("node_state" -> resource.nodeState.get)
          Http().singleRequest(HttpRequest(
            method = HttpMethods.POST,
            uri = s"http://$host:$port/resource",
            entity = HttpEntity(ContentTypes.`application/json`, body.toString())
          ))
          Behaviors.same
        }
      }
    }
