package it.unibo.citytwin.rivermonitor.view

import com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener
import it.unibo.citytwin.rivermonitor.actors.rivermonitor.RiverMonitorResourceState
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.{Evacuating, RiverMonitorState, Safe, Warned}
import it.unibo.citytwin.rivermonitor.model.{FloodSensor, RiverMonitor}
import upickle.default.{ReadWriter => RW, macroRW}
import upickle.default._
import java.awt.event.{WindowAdapter, WindowEvent}
import java.awt.{Dimension, Graphics2D, RenderingHints}
import javax.swing.{BorderFactory, SwingUtilities}
import scala.swing.BorderPanel.Position.{Center, North}
import scala.swing.{Action, BorderPanel, Button, FlowPanel, Frame, Label, Panel}

trait SwingControlPanel:
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
        implicit val rw: RW[RiverMonitorResourceState] = macroRW
        val riverMonitorResourceState: RiverMonitorResourceState = read[RiverMonitorResourceState](riverMonitorState)
        riverPanel.updateRiverMonitorState(riverMonitorResourceState)

        riverMonitorResourceState.riverMonitorState match
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
          case _ =>
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
  var riverMonitorResourceState: RiverMonitorResourceState = RiverMonitorResourceState("", 0, None)

  preferredSize = Dimension(width, height)

  override def paint(g: Graphics2D): Unit =
    val g2: Graphics2D = g
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g2.drawRect(0, 0, width - 1, height - 1)
    g2.setColor(java.awt.Color.BLUE)
    riverMonitorResourceState.riverMonitorState match
      case "Safe"       => g2.setColor(java.awt.Color.GREEN)
      case "Evacuating" => g2.setColor(java.awt.Color.YELLOW)
      case "Warned"     => g2.setColor(java.awt.Color.RED)
      case _            => g2.setColor(java.awt.Color.CYAN)
    g2.fillRect(0, 0, width, height)
    g2.setColor(java.awt.Color.BLACK)
    var yStringPosition: Int = 15
    g2.drawString(s"View name: ${viewName}", 5, yStringPosition)
    yStringPosition += 15
    g2.drawString(s"River monitor state: ${riverMonitorResourceState.riverMonitorState}", 5, yStringPosition)
    yStringPosition += 15
    g2.drawString(s"Water level threshold: ${riverMonitorResourceState.threshold}", 5, yStringPosition)
    yStringPosition += 15
    g2.drawRect(0, 0, width, height)
    g2.setColor(java.awt.Color.BLACK)
    riverMonitorResourceState.sensorsForView.getOrElse(Map())
      .foreach((sensorName, sensorData) =>
        //var sensorString: String = sensorName
        val sensorString: String = s"$sensorName - ${sensorData.map((key, value) => s"$key: $value").mkString(", ")}"
        g2.drawString(sensorString, 5, yStringPosition)
        yStringPosition += 15
    )
  end paint

  def updateRiverMonitorState(riverMonitorResourceState: RiverMonitorResourceState): Unit =
    this.riverMonitorResourceState = riverMonitorResourceState

end RiverPanel
