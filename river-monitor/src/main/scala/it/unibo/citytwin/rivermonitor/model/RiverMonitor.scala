package it.unibo.citytwin.rivermonitor.model

import it.unibo.citytwin.core.model.Point2D
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.RiverMonitorState
object RiverMonitorState extends Enumeration:
  type RiverMonitorState = Value
  val Safe, Evacuating, Warned = Value

case class RiverMonitor(position: Point2D[Int],
                       state: RiverMonitorState):

  def state_(newState: RiverMonitorState): RiverMonitor =
    RiverMonitor(position, newState)