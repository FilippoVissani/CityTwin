package it.unibo.citytwin.rivermonitor.view

import com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.{
  Evacuating,
  RiverMonitorState,
  Safe,
  Warned
}
import it.unibo.citytwin.rivermonitor.model.{FloodSensor, RiverMonitor}

import java.awt.event.{WindowAdapter, WindowEvent}
import java.awt.{Dimension, Graphics2D, RenderingHints}
import javax.swing.{BorderFactory, SwingUtilities}
import scala.swing.BorderPanel.Position.{Center, North}
import scala.swing.{Action, BorderPanel, Button, FlowPanel, Frame, Label, Panel}

trait SwingControlPanel:
  def updateFloodSensor(floodSensor: FloodSensor): Unit
  def updateRiverMonitorState(riverMonitorState: String): Unit

object SwingControlPanel:

  def apply(view: View): SwingControlPanel = SwingControlPanelImpl(view)

  private class SwingControlPanelImpl(view: View) extends Frame with SwingControlPanel:
    val buttonsPanel: ButtonsPanel = ButtonsPanel(view)
    val riverPanel: RiverPanel     = RiverPanel(view.width, view.height, view.viewName)

    title = "River Monitor Control Panel"
    resizable = false
    contents = new BorderPanel {
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

    // chiamato dalla View
    override def updateRiverMonitorState(riverMonitorState: String): Unit =
      SwingUtilities.invokeLater(() => {
        riverPanel.updateRiverMonitorState(riverMonitorState)

        riverMonitorState match
          case "Safe" => {
            buttonsPanel.buttonEvacuate.visible = false
            buttonsPanel.buttonEvacuated.visible = false
          }
          case "Warned" => {
            buttonsPanel.buttonEvacuate.visible = true
            buttonsPanel.buttonEvacuated.visible = false
          }
          case "Evacuating" => {
            buttonsPanel.buttonEvacuate.visible = false
            buttonsPanel.buttonEvacuated.visible = true
          }

        repaint()
      })

    override def updateFloodSensor(floodSensor: FloodSensor): Unit =
      SwingUtilities.invokeLater(() => {
        riverPanel.updateFloodSensor(floodSensor)
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
        visible = false
        view.evacuateZonePressed()
  }
  val buttonEvacuated: Button = new Button {
    text = "Zone Evacuated"
    visible = false
    action = new Action("Zone Evacuated"):
      override def apply(): Unit =
        visible = false
        view.evacuatedZonePressed()
  }
  contents += buttonEvacuate
  contents += buttonEvacuated

end ButtonsPanel

sealed class RiverPanel(width: Int, height: Int, viewName: String) extends Panel:
  var riverMonitorState: String       = "Safe"
  var floodSensors: List[FloodSensor] = List()

  preferredSize = Dimension(width, height)

  override def paint(g: Graphics2D): Unit =
    val g2: Graphics2D = g
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g2.drawRect(0, 0, width - 1, height - 1)
    g2.setColor(java.awt.Color.BLUE)
    riverMonitorState match
      case "Safe"       => g2.setColor(java.awt.Color.GREEN)
      case "Evacuating" => g2.setColor(java.awt.Color.YELLOW)
      case "Warned"     => g2.setColor(java.awt.Color.RED)
      case _            => g2.setColor(java.awt.Color.BLUE)
    g2.fillRect(0, 0, width, height)
    g2.setColor(java.awt.Color.BLACK)
    g2.drawString(s"RIVER MONITOR ${viewName}: ${riverMonitorState}", 5, 15)
    g2.drawRect(0, 0, width, height)
    g2.setColor(java.awt.Color.BLACK)
    floodSensors.foreach(floodSensor =>
      g2.fillOval(floodSensor.position.x, floodSensor.position.y, 10, 10)
    )
  end paint

  def updateFloodSensor(floodSensor: FloodSensor): Unit =
    this.floodSensors = floodSensor :: this.floodSensors.filter(x => x.name != floodSensor.name)
  def updateRiverMonitorState(riverMonitorState: String): Unit =
    this.riverMonitorState = riverMonitorState

end RiverPanel
