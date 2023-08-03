package it.unibo.citytwin.core.model

enum ResourceType:
  case Act
  case Sense

case class Resource(
    name: String = "",
    position: Option[Point2D[Int]] = None,
    state: Option[Any] = None,
    resourceType: Set[ResourceType] = Set(),
    nodeState: Boolean = true
)
