package it.unibo.citytwin.rivermonitor.view

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist
import it.unibo.citytwin.rivermonitor.actors.{EvacuatedZone, EvacuatingZone, ViewActorCommand}
import it.unibo.citytwin.rivermonitor.model.{FloodSensor, RiverMonitor, Zone}

trait View:
  def width: Int
  def height: Int
  def zoneId: Int
  def updateFloodSensor(floodSensor: FloodSensor): Unit
  def updateZone(zone: Zone): Unit
  def updateRiverMonitor(riverMonitor: RiverMonitor): Unit
  def evacuateZonePressed(): Unit
  def evacuatedZonePressed(): Unit

object View:
  def apply(width: Int, height: Int, zoneId: Int, viewActor: ActorRef[ViewActorCommand]): View =
    ViewImpl(width, height, zoneId, viewActor)

  /**
   * Implementation of View trait
   */
  private class ViewImpl(override val width: Int,
                         override val height: Int,
                         override val zoneId: Int,
                         val viewActor: ActorRef[ViewActorCommand]) extends View:
    val frame: SwingControlPanel = SwingControlPanel(this)

    override def updateZone(zone: Zone): Unit =
      frame.updateZone(zone)

    override def updateFloodSensor(floodSensor: FloodSensor): Unit =
      frame.updateFloodSensor(floodSensor)

    override def updateRiverMonitor(riverMonitor: RiverMonitor): Unit =
      frame.updateRiverMonitor(riverMonitor)

    override def evacuatedZonePressed(): Unit =
      viewActor ! EvacuatedZone

    override def evacuateZonePressed(): Unit =
      viewActor ! EvacuatingZone