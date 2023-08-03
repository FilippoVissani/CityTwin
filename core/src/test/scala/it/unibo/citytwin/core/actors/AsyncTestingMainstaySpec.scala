package it.unibo.citytwin.core.actors

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.receptionist.Receptionist
import akka.cluster.MemberStatus.Up
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.model.ResourceType.*
import it.unibo.citytwin.core.model.{Point2D, Resource}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AsyncTestingMainstaySpec extends AnyWordSpec with BeforeAndAfterAll with Matchers:

  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "Mainstay actor" should {
    "Register a resource state" in {
      val resource = Resource(
        name = Option("sensor1")
      )
      val dummyResourceActor = testKit.spawn(DummyResourceActor(), "dummyResource")
      val mainstay           = testKit.spawn(MainstayActor(), "mainstay")
      val probe              = testKit.createTestProbe[ResourceActorCommand]()
      mainstay ! UpdateResources(Map(dummyResourceActor -> resource))
      mainstay ! AskResourcesState(probe.ref, Set("sensor1"))
      probe.expectMessage(ResponseResourceState(Set(resource)))
      testKit.stop(mainstay)
      testKit.stop(dummyResourceActor)
    }

    "Register a new mainstay" in {
      val mainstay      = testKit.spawn(MainstayActor(), "mainstay")
      val mainstayState = Map(mainstay -> true)
      val probe         = testKit.createTestProbe[MainstayActorCommand]()
      probe ! SetMainstays(mainstayState)
      probe.expectMessage(SetMainstays(mainstayState))
      testKit.stop(mainstay)
    }

    "Merge resource states correctly" in {
      val onlineNodes = LazyList
        .from(0)
        .map(x =>
          (
            testKit.spawn(DummyResourceActor(), s"dummyResource$x"),
            Resource(name = Some(s"sensor$x"), nodeState = Some(true))
          )
        )
        .take(2)
        .toMap
      val offlineNodes = LazyList
        .from(2)
        .map(x =>
          (
            testKit.spawn(DummyResourceActor(), s"dummyResource$x"),
            Resource(name = Some(s"sensor$x"), nodeState = Some(false))
          )
        )
        .take(2)
        .toMap
      val expectedResult: Set[Resource] =
        onlineNodes.values.toSet - onlineNodes.values.head ++ offlineNodes.values.toSet + onlineNodes.values.head
          .merge(Resource(nodeState = Some(false)))
      val mainstay = testKit.spawn(MainstayActor(), "mainstay")
      val probe    = testKit.createTestProbe[ResourceActorCommand]()
      mainstay ! UpdateResources(onlineNodes)
      mainstay ! UpdateResources(
        offlineNodes + onlineNodes.head.copy(_2 = Resource(nodeState = Some(false)))
      )
      mainstay ! AskResourcesState(
        probe.ref,
        LazyList.from(0).map(x => s"sensor$x").take(10).toSet[String]
      )
      probe.expectMessage(ResponseResourceState(expectedResult))
      onlineNodes.foreach((k, _) => testKit.stop(k))
      offlineNodes.foreach((k, _) => testKit.stop(k))
      testKit.stop(mainstay)
    }
  }
