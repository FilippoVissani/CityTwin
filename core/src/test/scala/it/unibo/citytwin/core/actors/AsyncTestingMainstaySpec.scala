package it.unibo.citytwin.core.actors

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.MemberStatus.Up
import akka.util.Timeout
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.model.ResourceType.*
import it.unibo.citytwin.core.model.{Point2D, Resource}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class AsyncTestingMainstaySpec extends AnyWordSpec with BeforeAndAfterAll with Matchers:

  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "Mainstay actor" should {
    "Register a resource state" in {
      val resource = Resource(
        name = Option("sensor1")
      )
      val dummyResourceActor = testKit.spawn(DummyResourceActor())
      val mainstay           = testKit.spawn(MainstayActor("", ""))
      val probe              = testKit.createTestProbe[ResourceStatesResponse]()
      mainstay ! UpdateResources(Map(dummyResourceActor -> resource).toSet)
      mainstay ! AskResourcesState(probe.ref, Set("sensor1"))
      probe.expectMessage(ResourceStatesResponse(Set(resource)))
      testKit.stop(mainstay)
      testKit.stop(dummyResourceActor)
    }

    "Register a new mainstay" in {
      val mainstay      = testKit.spawn(MainstayActor("", ""))
      val mainstayState = Map(mainstay -> true)
      val probe         = testKit.createTestProbe[MainstayActorCommand]()
      probe ! SetMainstays(mainstayState.toSet)
      probe.expectMessage(SetMainstays(mainstayState.toSet))
      testKit.stop(mainstay)
    }

    "Sync with other mainstasy" in {
      val resource = Resource(
        name = Option("sensor1")
      )
      val mainstay = testKit.spawn(MainstayActor("", ""))
      val dummyResourceActor = testKit.spawn(DummyResourceActor())
      val probe = testKit.createTestProbe[MainstayActorCommand]()
      mainstay ! SetMainstays(Map(probe.ref -> true).toSet)
      mainstay ! UpdateResources(Map(dummyResourceActor -> resource).toSet)
      probe.expectMessage(Sync(Map(dummyResourceActor -> resource).toSet))
      testKit.stop(mainstay)
    }
  }
