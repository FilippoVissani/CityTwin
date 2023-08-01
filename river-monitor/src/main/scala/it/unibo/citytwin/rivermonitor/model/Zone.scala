package it.unibo.citytwin.rivermonitor.model

import it.unibo.citytwin.rivermonitor.model.ZoneState.ZoneState

object ZoneState extends Enumeration:
  type ZoneState = Value
  val Ok, Alarm, UnderManagement = Value

case class Zone(id: Int,
                bounds: Boundary,
                state: ZoneState):
  def state_(newState: ZoneState): Zone =
    Zone(id, bounds, newState)
