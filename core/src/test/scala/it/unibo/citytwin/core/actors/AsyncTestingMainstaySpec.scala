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
      val mainstay           = testKit.spawn(MainstayActor())
      val probe              = testKit.createTestProbe[ResourceStatesResponse]()
      mainstay ! UpdateResources(Map(dummyResourceActor -> resource).toSet)
      mainstay ! AskResourcesState(probe.ref, Set("sensor1"))
      probe.expectMessage(ResourceStatesResponse(Set(resource)))
      testKit.stop(mainstay)
      testKit.stop(dummyResourceActor)
    }

    "Register a new mainstay" in {
      val mainstay      = testKit.spawn(MainstayActor())
      val mainstayState = Map(mainstay -> true)
      val probe         = testKit.createTestProbe[MainstayActorCommand]()
      probe ! SetMainstays(mainstayState.toSet)
      probe.expectMessage(SetMainstays(mainstayState.toSet))
      testKit.stop(mainstay)
    }

    "Merge resource states correctly" in {
      val onlineNodes = LazyList
        .from(0)
        .map(x =>
          (
            testKit.spawn(DummyResourceActor()),
            Resource(name = Some(s"sensor$x"), nodeState = Some(true))
          )
        )
        .take(2)
        .toMap
      val offlineNodes = LazyList
        .from(2)
        .map(x =>
          (
            testKit.spawn(DummyResourceActor()),
            Resource(name = Some(s"sensor$x"), nodeState = Some(false))
          )
        )
        .take(2)
        .toMap
      val expectedResult: Set[Resource] =
        onlineNodes.values.toSet - onlineNodes.values.head ++ offlineNodes.values.toSet + onlineNodes.values.head
          .merge(Resource(nodeState = Some(false)))
      val mainstay = testKit.spawn(MainstayActor())
      val probe    = testKit.createTestProbe[ResourceActorCommand]()
      val mockedResourceBehavior = Behaviors.setup[ResourceActorCommand] { ctx =>
        implicit val timeout: Timeout = 3.seconds
        ctx.ask(
          mainstay,
          ref =>
            AskResourcesState(ref, LazyList.from(0).map(x => s"sensor$x").take(10).toSet[String])
        ) {
          case Success(ResourceStatesResponse(resources: Set[Resource])) =>
            AdaptedResourcesStateResponse(null, resources)
          case _ => AdaptedResourcesStateResponse(null, Set())
        }
        Behaviors.receiveMessage {
          case AdaptedResourcesStateResponse(
                _: ActorRef[ResourcesFromMainstayResponse],
                _: Set[Resource]
              ) => {
            Behaviors.same
          }
          case _ => Behaviors.stopped
        }
      }
      val probeActor = testKit.spawn(Behaviors.monitor(probe.ref, mockedResourceBehavior))
      mainstay ! UpdateResources(onlineNodes.toSet)
      mainstay ! UpdateResources(
        (offlineNodes + onlineNodes.head.copy(_2 = Resource(nodeState = Some(false)))).toSet
      )
      probe.expectMessage(AdaptedResourcesStateResponse(null, expectedResult))
      onlineNodes.foreach((k, _) => testKit.stop(k))
      offlineNodes.foreach((k, _) => testKit.stop(k))
      testKit.stop(mainstay)
      testKit.stop(probeActor)
    }

    "Inform other mainstays on Resource update" in {
      val probe    = testKit.createTestProbe[MainstayActorCommand]()
      val resource = testKit.spawn(DummyResourceActor())
      val mainstay = testKit.spawn(MainstayActor())
      mainstay ! SetMainstays(Map(probe.ref -> true).toSet)
      mainstay ! UpdateResources(Map(resource -> Resource()).toSet)
      probe.expectMessage(UpdateResources(Map(resource -> Resource()).toSet))
      testKit.stop(resource)
      testKit.stop(mainstay)
    }
  }
