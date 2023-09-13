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
import it.unibo.citytwin.core.model.ResourceType.{Act, ResourceType, Sense}
import it.unibo.citytwin.core.model.ResourceType
import it.unibo.citytwin.rivermonitor.model.{FloodSensorData, RiverMonitor, ViewData, ViewState}
import upickle._
import upickle.default._
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

        // Send monitored sensors to RiverMonitorStateActor, useful for the view
        val monitoredSensors = createMapOfMonitoredSensors(resources)
        riverMonitorStateActor ! MonitoredSensors(monitoredSensors)

        // Filter resources for sensors and actions
        val senseResources = filterResourcesByType(resources, Sense)
        val actResources   = filterResourcesByType(resources, Act)

        // elaborate sense resources
        if isNecessaryToWarn(senseResources, riverMonitor) then
          riverMonitorStateActor ! WarnRiverMonitor

        // elaborate act resources
        elaborateActResources(actResources, riverMonitorStateActor, ctx)

        Behaviors.same
      }
      case _ => {
        ctx.log.debug("Unexpected message. The actor is being stopped")
        Behaviors.stopped
      }
    }

  /** Filter resources by type
    *
    * @param resources
    *   A set of resources.
    * @param resourceType
    *   The type of resource to filter.
    * @return
    *   A set of resources of the specified type that are online and wich state is not empty.
    */
  private def filterResourcesByType(
      resources: Set[ResourceState],
      resourceType: ResourceType
  ): Set[ResourceState] =
    resources
      .filter(resource => resource.resourceType.contains(resourceType))
      .filter(resource => resource.nodeState.get)
      .filter(resource => resource.state.nonEmpty)

  /** Create a map of monitored sensors with their status and water level.
    *
    * @param resources
    *   A set of resources to extract sensor information from.
    * @return
    *   A map where the keys are sensor names, and the values are maps containing "Status" and
    *   "WaterLevel" entries.
    */
  private def createMapOfMonitoredSensors(
      resources: Set[ResourceState]
  ): Map[String, Map[String, String]] =
    resources
      .filter(resource => resource.resourceType.contains(Sense))
      .map(resource =>
        // get sensor name
        val name: String = resource.name.getOrElse("")
        // get sensor status
        val status: String = resource.nodeState
          .map(state => if (state) "online" else "offline")
          .getOrElse("")
        // get sensor water level
        val waterLevel: String = resource.state
          .map(jsonString => {
            try {
              val floodSensorData: FloodSensorData = read(jsonString)
              floodSensorData.waterLevel.toString
            } catch case _ => ""
          })
          .getOrElse("")
        // return a tuple (sensor name, map of sensor status and water level)
        (name, Map("Status" -> status, "WaterLevel" -> waterLevel))
      )
      .toMap // convert set of tuples to map

  /** Determine if it's necessary to send a warning to the RiverMonitorStateActor based on sensor
    * data.
    *
    * @param senseResources
    *   A set of sensed resources.
    * @param riverMonitor
    *   The RiverMonitor instance.
    * @return
    *   `true` if it's necessary to send a warning, `false` otherwise.
    */
  private def isNecessaryToWarn(
      senseResources: Set[ResourceState],
      riverMonitor: RiverMonitor
  ): Boolean =
    if senseResources.nonEmpty then
      // count sensors with water level above threshold
      val aboveThresholdCount = senseResources.count(resource =>
        resource.state.exists(jsonString =>
          try {
            val floodSensorData: FloodSensorData = read(jsonString)
            floodSensorData.waterLevel > riverMonitor.threshold
          } catch case _ => false
        )
      )
      // return true if more than half of the sensors are above threshold
      aboveThresholdCount > senseResources.size / 2
    else false

  /** Process act resources and send corresponding messages to the RiverMonitorStateActor.
    *
    * @param actResources
    *   A set of act resources to process.
    * @param riverMonitorStateActor
    *   The actor to send messages to.
    * @param ctx
    *   The actor context.
    */
  private def elaborateActResources(
      actResources: Set[ResourceState],
      riverMonitorStateActor: ActorRef[RiverMonitorStateActorCommand],
      ctx: ActorContext[RiverMonitorActorCommand]
  ): Unit =
    if actResources.nonEmpty then
      actResources.foreach(resource => {
        try {
          val actData: ViewData = read(resource.state.get)
          actData.state match
            case ViewState.Evacuating => riverMonitorStateActor ! EvacuatingRiverMonitor
            case ViewState.Safe       => riverMonitorStateActor ! EvacuatedRiverMonitor
            case null                 => ctx.log.debug("Unexpected ViewState")
        } catch case _ => ctx.log.debug("Unable to deserialize view state")
      })
