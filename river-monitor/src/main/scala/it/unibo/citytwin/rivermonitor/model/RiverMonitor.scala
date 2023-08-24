package it.unibo.citytwin.rivermonitor.model

import it.unibo.citytwin.core.model.Point2D
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.{RiverMonitorState, Safe}

/** Enumeration representing the possible states of a river monitor. */
object RiverMonitorState extends Enumeration:
  type RiverMonitorState = Value
  val Safe, Evacuating, Warned = Value

/** Represents a river monitor with its properties.
  * @param riverMonitorName
  *   the name of the river monitor
  * @param position
  *   the position of the river monitor
  * @param state
  *   the state of the river monitor
  * @param threshold
  *   the threshold of the river monitor
  */
case class RiverMonitor(
    riverMonitorName: String,
    position: Point2D[Int],
    state: RiverMonitorState = Safe,
    threshold: Float
)

/** Represents the state of a river monitor as a resource.
  *
  * @param riverMonitorState
  *   the state of the river monitor
  * @param threshold
  *   the threshold of the river monitor
  * @param monitoredSensors
  *   the monitored sensors
  */
case class RiverMonitorResourceState(
    riverMonitorState: String,
    threshold: Float,
    monitoredSensors: Option[Map[String, Map[String, String]]]
)
