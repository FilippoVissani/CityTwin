package it.unibo.citytwin.rivermonitor.view

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist
import it.unibo.citytwin.rivermonitor.actors.view.{EvacuatedZone, EvacuatingZone, ViewActorCommand}
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.RiverMonitorState
import it.unibo.citytwin.rivermonitor.model.{FloodSensor, RiverMonitor}

/** Defines the contract for a View, responsible for interacting with the UI and receiving updates
  * from actors.
  */
trait View:
  /** The width of the view's display area.
    */
  def width: Int

  /** The height of the view's display area.
    */
  def height: Int

  /** The name of the view.
    */
  def viewName: String

  /** The actor reference for communication with the associated ViewActor.
    */
  def viewActor: ActorRef[ViewActorCommand]

  /** Update the state of the river monitor, called by the view actor
    *
    * @param riverMonitorState
    *   The representation of the river monitor resource state
    */
  def updateRiverMonitorState(riverMonitorState: String): Unit

  /** Called when the "Evacuate" button is pressed
    */
  def evacuateZonePressed(): Unit

  /** Called when the "Evacuated" button is pressed
    */
  def evacuatedZonePressed(): Unit

/** Factory object for creating instances of View.
  */
object View:
  /** Creates an instance of View.
    *
    * @param width
    *   The width of the view's display area.
    * @param height
    *   The height of the view's display area.
    * @param viewName
    *   The name of the view.
    * @param viewActor
    *   The actor reference for communication with the associated ViewActor.
    * @return
    *   An instance of View.
    */
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
    /** The UI frame for the view, managing UI components and interactions.
      */
    val frame: SwingControlPanel = SwingControlPanel(this)

    override def updateRiverMonitorState(riverMonitorState: String): Unit =
      frame.updateRiverMonitorState(riverMonitorState)

    override def evacuateZonePressed(): Unit =
      viewActor ! EvacuatingZone

    override def evacuatedZonePressed(): Unit =
      viewActor ! EvacuatedZone
