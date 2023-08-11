package it.unibo.citytwin.rivermonitor

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.core.actors.{AskResourcesState, MainstayActorCommand, SetMainstayActorsToResourceActor, UpdateResources}
import it.unibo.citytwin.core.model.{Point2D, Resource}
import it.unibo.citytwin.core.model.ResourceType.{Act, Sense}
import it.unibo.citytwin.rivermonitor.actors.rivermonitor.{EvacuatedRiverMonitor, EvacuatingRiverMonitor, ResourceRiverMonitorActor, RiverMonitorActor, SetResourceActor, WarnRiverMonitor}
import it.unibo.citytwin.rivermonitor.model.RiverMonitor
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.Safe
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.core.actors.ResourceStatesResponse
import scala.concurrent.duration.DurationInt

class AsyncTestingRiverMonitorSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers:

  val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  "RiverMonitor" should {
    val mainstayProbe = testKit.createTestProbe[MainstayActorCommand]()

    val dummyRiverMonitor = RiverMonitor("riverMonitor1", Point2D[Int](0, 0), Safe)
    //Guardian work
    val riverMonitorActor = testKit.spawn(RiverMonitorActor(dummyRiverMonitor))
    val resourceRiverMonitorActor = testKit.spawn(ResourceRiverMonitorActor(riverMonitorActor = riverMonitorActor, resourcesToCheck = Set()))
    riverMonitorActor ! SetResourceActor(resourceRiverMonitorActor)
    resourceRiverMonitorActor ! SetMainstayActorsToResourceActor(Set(mainstayProbe.ref))
    //end Guardian work
    var dummyResource = Resource(Some(dummyRiverMonitor.riverMonitorName), Some(dummyRiverMonitor.position), Some(dummyRiverMonitor.state.toString), Set(Sense, Act))

    "send its state (Safe) to Mainstay when it's started" in {
      mainstayProbe.expectMessage(UpdateResources(Map(resourceRiverMonitorActor -> dummyResource).toSet))
    }
    "send its state (Warned) to Mainstay when receive WarnRiverMonitor" in {
      riverMonitorActor ! WarnRiverMonitor
      dummyResource = dummyResource.copy(state = Some("Warned"))
      mainstayProbe.expectMessage(UpdateResources(Map(resourceRiverMonitorActor -> dummyResource).toSet))
    }
    "send its state (Evacuating) to Mainstay when receive EvacuatingRiverMonitor" in {
      riverMonitorActor ! EvacuatingRiverMonitor
      dummyResource = dummyResource.copy(state = Some("Evacuating"))
      mainstayProbe.expectMessage(UpdateResources(Map(resourceRiverMonitorActor -> dummyResource).toSet))
    }
    "send its state (Safe) to Mainstay when receive EvacuatedRiverMonitor" in {
      riverMonitorActor ! EvacuatedRiverMonitor
      dummyResource = dummyResource.copy(state = Some("Safe"))
      mainstayProbe.expectMessage(UpdateResources(Map(resourceRiverMonitorActor -> dummyResource).toSet))
    }
  }

  "Resource RiverMonitor Actor" should {
    "ask resources to mainstay" in {
      val mainstayProbe = testKit.createTestProbe[MainstayActorCommand]()

      val dummyRiverMonitor = RiverMonitor("riverMonitor1", Point2D[Int](0, 0), Safe)
      //Guardian work
      val riverMonitorActor = testKit.spawn(RiverMonitorActor(dummyRiverMonitor))
      val resourceRiverMonitorActor = testKit.spawn(ResourceRiverMonitorActor(riverMonitorActor = riverMonitorActor, resourcesToCheck = Set()))
      resourceRiverMonitorActor ! SetMainstayActorsToResourceActor(Set(mainstayProbe.ref))
      //end Guardian work

      mainstayProbe.within(10.seconds) {
        mainstayProbe.expectMessageType[AskResourcesState]
      }
    }
  }