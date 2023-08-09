package it.unibo.citytwin.rivermonitor.view

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist
import it.unibo.citytwin.rivermonitor.actors.view.{EvacuatedZone, EvacuatingZone, ViewActorCommand}
import it.unibo.citytwin.rivermonitor.model.{FloodSensor, RiverMonitor}

trait View:
  def width: Int
  def height: Int
  def viewName: String
  def updateFloodSensor(floodSensor: FloodSensor): Unit
  def updateRiverMonitor(riverMonitor: RiverMonitor): Unit
  def evacuateZonePressed(): Unit
  def evacuatedZonePressed(): Unit

object View:
  def apply(width: Int, height: Int, viewName: String, viewActor: ActorRef[ViewActorCommand]): View =
    ViewImpl(width, height, viewName, viewActor)

  /**
   * Implementation of View trait
   */
  private class ViewImpl(override val width: Int,
                         override val height: Int,
                         override val viewName: String,
                         val viewActor: ActorRef[ViewActorCommand]) extends View:
    val frame: SwingControlPanel = SwingControlPanel(this)

    override def updateFloodSensor(floodSensor: FloodSensor): Unit =
      frame.updateFloodSensor(floodSensor)

    override def updateRiverMonitor(riverMonitor: RiverMonitor): Unit =
      frame.updateRiverMonitor(riverMonitor)

    override def evacuatedZonePressed(): Unit =
      viewActor ! EvacuatedZone

    override def evacuateZonePressed(): Unit =
      viewActor ! EvacuatingZone