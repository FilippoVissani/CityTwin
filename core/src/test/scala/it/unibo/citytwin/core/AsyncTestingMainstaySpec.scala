package it.unibo.citytwin.core

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.receptionist.Receptionist
import akka.cluster.MemberStatus.Up
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.model.{Point2D, Resource}
import it.unibo.citytwin.core.model.ResourceType.*
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AsyncTestingMainstaySpec
    extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers:

  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "Mainstay actor" should {
    "Register a resource state" in {
      val resource = Resource(
        name = "sensor1",
        position = Some(Point2D(0, 0)),
        state = Option(1),
        resourceType = Set(Sense),
      )
      val mainstay = testKit.spawn(MainstayActor(), "mainstay")
      val probe = testKit.createTestProbe[ResourceActorCommand]()
      mainstay ! UpdateResourceState(resource)
      mainstay ! AskResourceState(probe.ref, "sensor1")
      probe.expectMessage(ResponseResourceState(Option(resource)))
    }

    "Register a new mainstay" in {
      val mainstay = testKit.spawn(MainstayActor(), "mainstay")
      val probe = testKit.createTestProbe[MainstayActorCommand]()
      probe ! SetMainstayActors(Set(mainstay))
      probe.expectMessage(SetMainstayActors(Set(mainstay)))
    }
  }
