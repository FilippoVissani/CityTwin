package it.unibo.citytwin.core.model

import it.unibo.citytwin.core.model.ResourceType.ResourceType
import play.api.libs.json.JsValue

object ResourceType extends Enumeration:
  type ResourceType = Value
  val Act, Sense = Value

case class Resource(
    name: Option[String] = None,
    position: Option[Point2D[Int]] = None,
    state: Option[JsValue] = None,
    resourceType: Set[ResourceType] = Set(),
    nodeState: Option[Boolean] = None
):
  def merge(other: Resource): Resource =
    val name     = if other.name.isDefined then other.name else this.name
    val position = if other.position.isDefined then other.position else this.position
    val state    = if other.state.isDefined then other.state else this.state
    val resourceType =
      if other.resourceType.nonEmpty then other.resourceType else this.resourceType
    val nodeState = if other.nodeState.isDefined then other.nodeState else this.nodeState
    Resource(name, position, state, resourceType, nodeState)
