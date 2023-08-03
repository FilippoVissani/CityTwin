package it.unibo.citytwin.core.model

enum ResourceType:
  case Act
  case Sense

trait Resource:
  def name: Option[String]
  def position: Option[Point2D[Int]]
  def state: Option[Any]
  def resourceType: Set[ResourceType]
  def nodeState: Option[Boolean]
  def merge(other: Resource): Resource

object Resource:
  def apply(
      name: Option[String] = None,
      position: Option[Point2D[Int]] = None,
      state: Option[Any] = None,
      resourceType: Set[ResourceType] = Set(),
      nodeState: Option[Boolean] = None
  ): Resource = ResourceImpl(name, position, state, resourceType, nodeState)

  private case class ResourceImpl(
      override val name: Option[String],
      override val position: Option[Point2D[Int]],
      override val state: Option[Any],
      override val resourceType: Set[ResourceType],
      override val nodeState: Option[Boolean]
  ) extends Resource:
    override def merge(other: Resource): Resource =
      val name     = if other.name.isDefined then other.name else this.name
      val position = if other.position.isDefined then other.position else this.position
      val state    = if other.state.isDefined then other.state else this.state
      val resourceType =
        if other.resourceType.nonEmpty then other.resourceType else this.resourceType
      val nodeState = if other.nodeState.isDefined then other.nodeState else this.nodeState
      Resource(name, position, state, resourceType, nodeState)
