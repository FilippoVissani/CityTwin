package it.unibo.citytwin.rivermonitor.model

import it.unibo.citytwin.core.model.Point2D
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.Safe
import upickle.default.ReadWriter

/** Enumeration representing the possible states of a river monitor. */
enum RiverMonitorState derives ReadWriter:
  case Safe, Evacuating, Warned

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

/** Represents the data of a river monitor as a resource.
  *
  * @param riverMonitorState
  *   the state of the river monitor
  * @param threshold
  *   the threshold of the river monitor
  * @param monitoredSensors
  *   the monitored sensors
  */
case class RiverMonitorData(
    riverMonitorState: RiverMonitorState,
    threshold: Float,
    monitoredSensors: Option[Map[String, Map[String, String]]]
) derives ReadWriter
