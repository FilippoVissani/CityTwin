package it.unibo.citytwin.rivermonitor.model

import it.unibo.citytwin.core.model.Point2D
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.{RiverMonitorState, Safe}
object RiverMonitorState extends Enumeration:
  type RiverMonitorState = Value
  val Safe, Evacuating, Warned = Value

case class RiverMonitor(
    riverMonitorName: String,
    position: Point2D[Int],
    state: RiverMonitorState = Safe
)