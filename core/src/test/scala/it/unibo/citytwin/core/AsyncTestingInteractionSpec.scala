package it.unibo.citytwin.core

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import it.unibo.citytwin.core.actors.MainstayActor
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AsyncTestingInteractionSpec
  extends AnyWordSpec
  with BeforeAndAfterAll
  with Matchers:

  val testKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "Mainstay actor" should {
    "name test" in {
      val mainstay = testKit.spawn(MainstayActor(), "mainstay")
    }
  }
