package it.unibo.citytwin.rivermonitor

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import it.unibo.citytwin.core.actors.{MainstayActorCommand, SetMainstayActorsToResourceActor, UpdateResources}
import it.unibo.citytwin.core.model.Point2D
import it.unibo.citytwin.rivermonitor.actors.floodsensor.{FloodSensorActor, ResourceFloodSensorActor, SetResourceActor}
import it.unibo.citytwin.rivermonitor.model.FloodSensor

import scala.concurrent.duration.DurationInt

class AsyncTestingFloodSensorSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers:

  val testKit: ActorTestKit = ActorTestKit()
  override def afterAll(): Unit = testKit.shutdownTestKit()

  "FloodSensor" should {
    "send measurement to Mainstay" in {
      val mainstayProbe = testKit.createTestProbe[MainstayActorCommand]()

      val dummyFloodSensor = FloodSensor("sensor1", Point2D[Int](0, 0))
      val floodSensorActor = testKit.spawn(FloodSensorActor(dummyFloodSensor))
      val resourceFloodSensorActor = testKit.spawn(ResourceFloodSensorActor(floodSensorActor))
      floodSensorActor ! SetResourceActor(resourceFloodSensorActor)
      resourceFloodSensorActor ! SetMainstayActorsToResourceActor(Set(mainstayProbe.ref))

      mainstayProbe.within(10.seconds){
        mainstayProbe.expectMessageType[UpdateResources]
      }
    }
  }
