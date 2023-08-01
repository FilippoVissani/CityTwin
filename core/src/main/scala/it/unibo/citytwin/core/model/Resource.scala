package it.unibo.citytwin.core.model

enum ResourceType:
  case Act
  case Sense

enum NodeState:
  case UP
  case DOWN

case class Resource(name: String,
                    position: Option[Point2D[Int]],
                    state: Option[Any],
                    resourceType: Set[ResourceType],
                    nodeState: NodeState = NodeState.UP)
