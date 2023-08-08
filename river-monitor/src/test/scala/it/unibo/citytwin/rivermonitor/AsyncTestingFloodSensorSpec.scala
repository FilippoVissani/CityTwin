package it.unibo.citytwin.rivermonitor

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.receptionist.Receptionist
import it.unibo.citytwin.core.actors.{MainstayActor, MainstayActorCommand, SetMainstayActorsToResourceActor, SetResourceState}
import it.unibo.citytwin.core.actors.ResourceActor.resourceService
import it.unibo.citytwin.core.model.{Point2D, Resource, ResourceType}
import it.unibo.citytwin.rivermonitor.actors.floodsensor.{FloodSensorActor, FloodSensorGuardianActor, ResourceFloodSensorActor}
import it.unibo.citytwin.rivermonitor.model.FloodSensor

import scala.concurrent.duration.DurationInt

class AsyncTestingFloodSensorSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers:

  val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  "FloodSensor" should {
    "send measurement to Mainstay" in {
      //val mainstayActor = testKit.spawn(MainstayActor())
      val mainstayProbe = testKit.createTestProbe[MainstayActorCommand]()

      //val floodSensorGuardianActor = testKit.spawn(FloodSensorGuardianActor(dummyFloodSensor))
      val dummyFloodSensor = FloodSensor("sensor1", Point2D[Int](0, 0))
      val floodSensorActor = testKit.spawn(FloodSensorActor(dummyFloodSensor)) //used to start resourceFloodSensorActor

      val resourceFloodSensorActor = testKit.spawn(ResourceFloodSensorActor(floodSensorActor))
      val dummyResource = Resource("sensor1", Some(Point2D[Int](0, 0)), Some(2), Set(ResourceType.Sense))

      resourceFloodSensorActor ! SetMainstayActorsToResourceActor(Set(mainstayProbe.ref))
      //mainstayProbe ! SetResourceState(resourceFloodSensorActor, Some(dummyResource))
      mainstayProbe.within(10.seconds){
        mainstayProbe.expectMessageType[SetResourceState]
      }

      //testKit.stop(floodSensorGuardianActor)
    }
  }
