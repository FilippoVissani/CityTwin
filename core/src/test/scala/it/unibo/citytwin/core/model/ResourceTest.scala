package it.unibo.citytwin.core.model

import it.unibo.citytwin.core.model.ResourceType.*
import it.unibo.citytwin.core.model.{Point2D, Resource}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ResourceTest extends AnyWordSpec with Matchers:
  "Resource" should {
    "Merge correctly another resource" in {
      val resource1 =
        Resource(name = Some("sensor1"), nodeState = Some(true), position = Some(Point2D(0, 0)))
      val resource2 =
        Resource(name = Some("sensor1"), resourceType = Set(Act), position = Some(Point2D(1, 1)))
      val result = Resource(
        name = Some("sensor1"),
        resourceType = Set(Act),
        nodeState = Some(true),
        position = Some(Point2D(1, 1))
      )
      resource1.merge(resource2) shouldBe result
    }
  }
