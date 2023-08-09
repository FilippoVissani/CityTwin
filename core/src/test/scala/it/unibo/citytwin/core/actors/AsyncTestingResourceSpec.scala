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

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class AsyncTestingResourceSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers:

  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "Resource actor" should {

    "Ask resources to mainstay" in {
      val probe         = testKit.createTestProbe[MainstayActorCommand]()
      val resourceActor = testKit.spawn(ResourceActor(), "resourceActor")
      val resource      = Resource(name = Some("sensor1"))
      resourceActor ! SetMainstayActorsToResourceActor(Set(probe.ref))
      resourceActor ! ResourceChanged(resource)
      probe.expectMessage(UpdateResources(Set((resourceActor, resource))))
      testKit.stop(resourceActor)
    }

    "Send response when resource state is asked" in {
      val mainstayActor = testKit.spawn(MainstayActor(), "mainstayActor")
      val resourceActor = testKit.spawn(ResourceActor(), "resourceActor")
      val probe         = testKit.createTestProbe[ResourcesFromMainstayResponse]()
      val resource      = Resource(name = Some("sensor1"), nodeState = Some(true))
      resourceActor ! SetMainstayActorsToResourceActor(Set(mainstayActor))
      resourceActor ! ResourceChanged(resource)
      resourceActor ! AskResourcesToMainstay(probe.ref, Set("sensor1"))
      probe.expectMessage(ResourcesFromMainstayResponse(Set(resource)))
      testKit.stop(resourceActor)
      testKit.stop(mainstayActor)
    }

    "Set mainstays correctly and notify new resource state to mainstay" in {
      val probe    = testKit.createTestProbe[MainstayActorCommand]()
      val resource = testKit.spawn(ResourceActor(), "resource")
      resource ! SetMainstayActorsToResourceActor(Set(probe.ref))
      resource ! ResourceChanged(Resource())
      probe.expectMessage(UpdateResources(Set((resource, Resource()))))
      testKit.stop(resource)
    }
  }
