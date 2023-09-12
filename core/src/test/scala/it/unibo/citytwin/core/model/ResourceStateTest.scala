package it.unibo.citytwin.core.model

import it.unibo.citytwin.core.model.ResourceType.*
import it.unibo.citytwin.core.model.{Point2D, ResourceState}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDateTime

class ResourceStateTest extends AnyWordSpec with Matchers:
  "Resource" should {
    "Merge correctly another resource" in {
      val resource1 =
        ResourceState(
          name = Some("sensor1"),
          nodeState = Some(true),
          position = Some(Point2D(0, 0)),
          time = Some(LocalDateTime.MAX)
        )
      val resource2 =
        ResourceState(
          name = Some("sensor1"),
          resourceType = Set(Act),
          position = Some(Point2D(1, 1)),
          time = Some(LocalDateTime.MIN)
        )
      val result = ResourceState(
        name = Some("sensor1"),
        resourceType = Set(Act),
        nodeState = Some(true),
        position = Some(Point2D(0, 0)),
        time = Some(LocalDateTime.MAX)
      )
      resource1.merge(resource2) shouldBe result
    }
  }
