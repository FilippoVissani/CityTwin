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

class AsyncTestingMainstaySpec extends AnyWordSpec with BeforeAndAfterAll with Matchers:

  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "Mainstay actor" should {
    "Register a resource state" in {
      val resource = Resource(
        name = "sensor1",
        position = Some(Point2D(0, 0)),
        state = Some(1),
        resourceType = Set(Sense)
      )
      val dummyResourceActor = testKit.spawn(DummyResourceActor(), "dummyResource")
      val mainstay           = testKit.spawn(MainstayActor(), "mainstay")
      val probe              = testKit.createTestProbe[ResourceActorCommand]()
      mainstay ! SetResourceState(dummyResourceActor, resource)
      mainstay ! AskResourcesState(probe.ref, Set("sensor1"))
      probe.expectMessage(ResponseResourceState(Set(resource)))
      testKit.stop(mainstay)
      testKit.stop(dummyResourceActor)
    }

    "Register a new mainstay" in {
      val mainstay      = testKit.spawn(MainstayActor(), "mainstay")
      val mainstayState = Map(mainstay -> true)
      val probe         = testKit.createTestProbe[MainstayActorCommand]()
      probe ! SetMainstayActors(mainstayState)
      probe.expectMessage(SetMainstayActors(mainstayState))
      testKit.stop(mainstay)
    }
  }
