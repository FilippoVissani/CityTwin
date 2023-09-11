package it.unibo.citytwin.core.model

import it.unibo.citytwin.core.model.ResourceType.ResourceType

import java.time.LocalDateTime
import scala.math.Ordered.orderingToOrdered

/** ResourceType is the enumeration that represents the type of a resource
  */
object ResourceType extends Enumeration:
  type ResourceType = Value
  val Act, Sense = Value

/** Resource is the class that represents a resource
  * @param name
  *   the name of the resource
  * @param position
  *   the position of the resource
  * @param state
  *   the state of the resource
  * @param resourceType
  *   the type of the resource
  * @param nodeState
  *   the state of the node
  */
case class ResourceState(
    name: Option[String] = None,
    position: Option[Point2D[Int]] = None,
    state: Option[String] = None,
    resourceType: Set[ResourceType] = Set(),
    nodeState: Option[Boolean] = None,
    time: Option[LocalDateTime] = None
):
  def merge(other: ResourceState): ResourceState =
    if other.time.isDefined && this.time.isDefined then
      if other.time.get > this.time.get then
        mergeFields(other, this)
      else
        mergeFields(this, other)
    else throw new Exception("Time of resource state must be defined")

  private def mergeFields(r1: ResourceState, r2: ResourceState): ResourceState =
    val name = if r1.name.isDefined then r1.name else r2.name
    val position = if r1.position.isDefined then r1.position else r2.position
    val state = if r1.state.isDefined then r1.state else r2.state
    val resourceType = if r1.resourceType.nonEmpty then r1.resourceType else r2.resourceType
    val nodeState = if r1.nodeState.isDefined then r1.nodeState else r2.nodeState
    val time = if r1.time.isDefined then r1.time else r2.time
    ResourceState(name, position, state, resourceType, nodeState, time)
