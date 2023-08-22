package it.unibo.citytwin.rivermonitor.view

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist
import it.unibo.citytwin.rivermonitor.actors.view.{EvacuatedZone, EvacuatingZone, ViewActorCommand}
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.RiverMonitorState
import it.unibo.citytwin.rivermonitor.model.{FloodSensor, RiverMonitor}

trait View:
  def width: Int
  def height: Int
  def viewName: String
  def viewActor: ActorRef[ViewActorCommand]

  /** Update the state of the river monitor, called by the view actor
    *
    * @param riverMonitorState
    */
  def updateRiverMonitorState(riverMonitorState: String): Unit

  /** Called when the "Evacuate" button is pressed
    */
  def evacuateZonePressed(): Unit

  /** Called when the "Evacuated" button is pressed
    */
  def evacuatedZonePressed(): Unit

object View:
  def apply(
      width: Int,
      height: Int,
      viewName: String,
      viewActor: ActorRef[ViewActorCommand]
  ): View =
    ViewImpl(width, height, viewName, viewActor)

  /** Implementation of View trait
    */
  private class ViewImpl(
      override val width: Int,
      override val height: Int,
      override val viewName: String,
      override val viewActor: ActorRef[ViewActorCommand]
  ) extends View:
    val frame: SwingControlPanel = SwingControlPanel(this)

    override def updateRiverMonitorState(riverMonitorState: String): Unit =
      frame.updateRiverMonitorState(riverMonitorState)

    override def evacuateZonePressed(): Unit =
      viewActor ! EvacuatingZone

    override def evacuatedZonePressed(): Unit =
      viewActor ! EvacuatedZone
