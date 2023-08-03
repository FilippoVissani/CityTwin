package it.unibo.citytwin.rivermonitor.view

import com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener
import it.unibo.citytwin.rivermonitor.model.{FloodSensor, RiverMonitor, Zone}
import it.unibo.citytwin.rivermonitor.model.ZoneState.*
import java.awt.event.{WindowAdapter, WindowEvent}
import java.awt.{Dimension, Graphics2D, RenderingHints}
import javax.swing.{BorderFactory, SwingUtilities}
import scala.swing.BorderPanel.Position.{Center, North}
import scala.swing.{Action, BorderPanel, Button, FlowPanel, Frame, Label, Panel}

trait SwingControlPanel:
  def updateFloodSensor(floodSensor: FloodSensor): Unit
  def updateZone(zone: Zone): Unit
  def updateRiverMonitor(riverMonitor: RiverMonitor): Unit

object SwingControlPanel:

  def apply(view: View): SwingControlPanel = SwingControlPanelImpl(view)

  private class SwingControlPanelImpl(view: View) extends Frame with SwingControlPanel:
    val buttonsPanel: ButtonsPanel = ButtonsPanel(view)
    val riverPanel: RiverPanel = RiverPanel(view.width, view.height)

    title = "River Monitor Control Panel"
    resizable = false
    contents = new BorderPanel{
      layout(buttonsPanel) = North
      layout(riverPanel) = Center
    }
    visible = true

    addWindowListener(new WindowAdapter() {
      override def windowClosing(ev: WindowEvent): Unit =
        System.exit(-1)

      override def windowClosed(ev: WindowEvent): Unit =
        System.exit(-1)
    })

    override def updateRiverMonitor(riverMonitor: RiverMonitor): Unit =
      SwingUtilities.invokeLater(() => {
        riverPanel.updateRiverMonitor(riverMonitor)
        repaint()
      })

    override def updateFloodSensor(floodSensor: FloodSensor): Unit =
      SwingUtilities.invokeLater(() => {
        riverPanel.updateFloodSensor(floodSensor)
        repaint()
      })

    override def updateZone(zone: Zone): Unit =
      SwingUtilities.invokeLater(() => {
        riverPanel.updateZone(zone)
        if view.zoneId == zone.id && zone.state == Alarm then
          buttonsPanel.buttonEvacuate.visible = true
        repaint()
      })

  end SwingControlPanelImpl
end SwingControlPanel

sealed class ButtonsPanel(view: View) extends FlowPanel:
  val buttonEvacuate: Button = new Button {
    text = "Evacuate Zone"
    visible = false
    action = new Action("Evacuate Zone"):
      override def apply(): Unit =
        buttonEvacuated.visible = true
        visible = false
        view.evacuateZonePressed()
  }
  val buttonEvacuated: Button = new Button{
    text = "Zone Evacuated"
    visible = false
    action = new Action("Zone Evacuated"):
      override def apply(): Unit =
        visible = false
        view.evacuatedZonePressed()
  }
  val zoneIdLabel: Label = Label(s"Zone ${view.zoneId}")
  contents += zoneIdLabel
  contents += buttonEvacuate
  contents += buttonEvacuated

end ButtonsPanel

sealed class RiverPanel(width: Int, height: Int) extends Panel:
  var RiverMonitors: List[RiverMonitor] = List()
  var zones: List[Zone] = List()
  var floodSensors: List[FloodSensor] = List()

  preferredSize = Dimension(width, height)

  override def paint(g: Graphics2D): Unit =
    val g2: Graphics2D = g
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g2.drawRect(0, 0, width - 1, height - 1)
    g2.setColor(java.awt.Color.BLUE)
    zones.foreach(zone => {
      zone match
        case Zone(_, _, Ok) => g2.setColor(java.awt.Color.GREEN)
        case Zone(_, _, UnderManagement) => g2.setColor(java.awt.Color.YELLOW)
        case Zone(_, _, Alarm) => g2.setColor(java.awt.Color.RED)
      g2.fillRect(zone.bounds.x0, zone.bounds.y0, zone.bounds.width, zone.bounds.height)
      g2.setColor(java.awt.Color.BLACK)
      g2.drawString(s"ZONE ${zone.id}: ${zone.state.toString}", zone.bounds.x0 + 5, zone.bounds.y0 + 15)
      g2.drawRect(zone.bounds.x0, zone.bounds.y0, zone.bounds.width, zone.bounds.height)
    })
    g2.setColor(java.awt.Color.BLACK)
    floodSensors.foreach(floodSensor => g2.fillOval(floodSensor.position.x, floodSensor.position.y, 10, 10))
    RiverMonitors.foreach(riverMonitor => {
      g2.fillRect(riverMonitor.position.x, riverMonitor.position.y, 10, 10)
      g2.drawString(riverMonitor.state.toString, riverMonitor.position.x, riverMonitor.position.y + 20)
    })
  end paint

  def updateFloodSensor(floodSensor: FloodSensor): Unit =
    this.floodSensors = floodSensor :: this.floodSensors.filter(x => x.position != floodSensor.position)
  def updateZone(zone: Zone): Unit =
    this.zones = zone :: this.zones.filter(x => x.id != zone.id)
  def updateRiverMonitor(riverMonitor: RiverMonitor): Unit =
    this.RiverMonitors = riverMonitor :: this.RiverMonitors.filter(x => x.zoneId != riverMonitor.zoneId)

end RiverPanel