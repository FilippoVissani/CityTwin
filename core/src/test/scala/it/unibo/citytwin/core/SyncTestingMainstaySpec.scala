package it.unibo.citytwin.core

import akka.actor.testkit.typed.Effect.SpawnedAnonymous
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, BehaviorTestKit}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.MemberStatus.Up
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.actors.MainstayActor.mainstayService
import it.unibo.citytwin.core.model.ResourceType.*
import it.unibo.citytwin.core.model.{Point2D, Resource}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SyncTestingMainstaySpec
    extends AnyWordSpec
    with Matchers:

  "Mainstay guardian actor" should {
    "Spawn new mainstay actor" in {
      val testKit = BehaviorTestKit(MainstayGuardianActor())
      val mainstay = Behaviors.receiveMessage[MainstayActorCommand] { _ =>
        Behaviors.same[MainstayActorCommand]
      }
      testKit.expectEffect(SpawnedAnonymous(mainstay))
    }
  }
