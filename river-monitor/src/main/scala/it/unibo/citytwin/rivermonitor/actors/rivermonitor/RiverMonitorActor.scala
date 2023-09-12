package it.unibo.citytwin.rivermonitor.actors.rivermonitor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.actors.AskResourcesToMainstay
import it.unibo.citytwin.core.actors.ResourceActor
import it.unibo.citytwin.core.actors.ResourceActorCommand
import it.unibo.citytwin.core.actors.ResourcesFromMainstayResponse
import it.unibo.citytwin.core.model.ResourceState
import it.unibo.citytwin.core.model.ResourceType.Act
import it.unibo.citytwin.core.model.ResourceType.Sense
import it.unibo.citytwin.rivermonitor.model.RiverMonitor
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.*

import scala.util.Success

import concurrent.duration.DurationInt

/** Command trait for messages that the RiverMonitorActor can receive.
  */
trait RiverMonitorActorCommand

/** A message representing a periodic tick event for the RiverMonitorActor. This is used to trigger
  * the RiverMonitorActor to perform periodic tasks.
  */
case class Tick(resourcesToCheck: Set[String]) extends Serializable with RiverMonitorActorCommand

/** Message received as a response when asking resources status
  *
  * @param resources
  *   a set containing requested resources
  */
case class AdaptedResourcesStateResponse(resources: Set[ResourceState])
    extends Serializable
    with RiverMonitorActorCommand

/** An actor responsible for simulating a river monitor behavior.
  */
object RiverMonitorActor:
  /** Factory method to create a new RiverMonitorActor.
    *
    * @param riverMonitor
    *   The RiverMonitor instance associated with the actor.
    * @param resourcesToCheck
    *   A set of resource names to periodically check.
    * @return
    *   Behavior[RiverMonitorActorCommand]
    */
  def apply(
      riverMonitor: RiverMonitor,
      resourcesToCheck: Set[String]
  ): Behavior[RiverMonitorActorCommand] =
    Behaviors.setup[RiverMonitorActorCommand] { ctx =>
      implicit val timeout: Timeout = 3.seconds
      val resourceActor             = ctx.spawnAnonymous(ResourceActor())
      val riverMonitorStateActor =
        ctx.spawnAnonymous(RiverMonitorStateActor(riverMonitor, resourceActor))
      // Set up timers for periodic Tick messages
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Tick(resourcesToCheck), 1.seconds)
        RiverMonitorActorLogic(ctx, riverMonitorStateActor, resourceActor, riverMonitor)
      }
    }

  // Private method to define the behavior of the RiverMonitorActor
  private def RiverMonitorActorLogic(
      ctx: ActorContext[RiverMonitorActorCommand],
      riverMonitorStateActor: ActorRef[RiverMonitorStateActorCommand],
      resourceActor: ActorRef[ResourceActorCommand],
      riverMonitor: RiverMonitor
  ): Behavior[RiverMonitorActorCommand] =
    implicit val timeout: Timeout = 3.seconds
    Behaviors.receiveMessage {
      case Tick(resourcesToCheck) => {
        ctx.log.debug("Received Tick")
        // Request resource status from the ResourceActor using AskResourcesToMainstay message
        ctx.ask(resourceActor, ref => AskResourcesToMainstay(ref, resourcesToCheck)) {
          case Success(ResourcesFromMainstayResponse(resources: Set[ResourceState])) =>
            AdaptedResourcesStateResponse(resources)
          case _ => {
            ctx.log.debug("Resources not received. Actor is unreachable.")
            AdaptedResourcesStateResponse(Set())
          }
        }
        Behaviors.same
      }
      case AdaptedResourcesStateResponse(resources) => {
        ctx.log.debug("Received AdaptedResourcesStateResponse")
        // Filter resources for sensors and actions
        val senseResources = resources
          .filter(resource => resource.resourceType.contains(Sense))
          .filter(resource => resource.nodeState.get)
          .filter(resource => resource.state.nonEmpty)
        val actResources = resources
          .filter(resource => resource.resourceType.contains(Act))
          .filter(resource => resource.nodeState.get)
          .filter(resource => resource.state.nonEmpty)

        // Create a map of monitored sensors
        val monitoredSensors: Map[String, Map[String, String]] = resources
          .filter(resource => resource.resourceType.contains(Sense))
          .map(resource =>
            resource.name.getOrElse("") -> Map(
              "Status" -> resource.nodeState
                .map(state => if (state) "online" else "offline")
                .getOrElse(""),
              "WaterLevel" -> resource.state.getOrElse("")
            )
          )
          .toMap
        // Send MonitoredSensors message to the RiverMonitorStateActor
        riverMonitorStateActor ! MonitoredSensors(monitoredSensors)

        // Check conditions and send appropriate messages to the RiverMonitorStateActor
        if senseResources.nonEmpty then
          if senseResources.count(resource =>
              resource.state.get.toFloat > riverMonitor.threshold
            ) > senseResources.size / 2
          then riverMonitorStateActor ! WarnRiverMonitor

        if actResources.nonEmpty then
          actResources.foreach(resource => {
            resource.state.getOrElse("") match
              case "Evacuating" => riverMonitorStateActor ! EvacuatingRiverMonitor
              case "Safe"       => riverMonitorStateActor ! EvacuatedRiverMonitor
              case _            => ctx.log.debug("Unexpected message")
          })
        Behaviors.same
      }
      case _ => {
        ctx.log.debug("Unexpected message. The actor is being stopped")
        Behaviors.stopped
      }
    }
