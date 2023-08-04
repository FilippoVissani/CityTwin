package it.unibo.citytwin.core.actors

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist
import akka.cluster.MemberStatus.Up
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.model.ResourceType.*
import it.unibo.citytwin.core.model.{Point2D, Resource}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AsyncTestingNodesObserverSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers:

  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "Nodes observer actor" should {
    "Send resource updates to mainstay correctly" in {
      val probe    = testKit.createTestProbe[MainstayActorCommand]()
      val observer = testKit.spawn(NodesObserverActor(probe.ref), "observer")
      val resource = testKit.spawn(DummyResourceActor(), "resource")
      observer ! UpdateResourceNodesState(Set(resource))
      probe.expectMessage(UpdateResources(Map(resource -> Resource(nodeState = Some(true)))))
      observer ! UpdateResourceNodesState(Set())
      probe.expectMessage(UpdateResources(Map(resource -> Resource(nodeState = Some(false)))))
      testKit.stop(observer)
      testKit.stop(resource)
    }

    "Send mainstay updates to mainstay correctly" in {
      val probe    = testKit.createTestProbe[MainstayActorCommand]()
      val observer = testKit.spawn(NodesObserverActor(probe.ref), "observer")
      val mainstay = testKit.spawn(MainstayActor(), "mainstay")
      observer ! UpdateMainstayNodesState(Set(mainstay))
      probe.expectMessage(SetMainstays(Map(mainstay -> true)))
      observer ! UpdateMainstayNodesState(Set())
      probe.expectMessage(SetMainstays(Map(mainstay -> false)))
      testKit.stop(observer)
      testKit.stop(mainstay)
    }
  }
