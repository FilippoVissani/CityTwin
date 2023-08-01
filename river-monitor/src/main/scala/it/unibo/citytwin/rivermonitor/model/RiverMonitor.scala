package it.unibo.citytwin.rivermonitor.model

import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.RiverMonitorState
object RiverMonitorState extends Enumeration:
  type RiverMonitorState = Value
  val Free, Evacuating, Warned = Value

case class RiverMonitor(zoneId: Int,
                       position: Point2D,
                       state: RiverMonitorState):

  def state_(newState: RiverMonitorState): RiverMonitor =
    RiverMonitor(zoneId, position, newState)