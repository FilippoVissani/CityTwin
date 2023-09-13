package it.unibo.citytwin.rivermonitor.view

import com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener
import it.unibo.citytwin.rivermonitor.model.{RiverMonitorData, RiverMonitorState}
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.SwingUtilities
import scala.swing.Action
import scala.swing.BorderPanel
import scala.swing.BorderPanel.Position.Center
import scala.swing.BorderPanel.Position.North
import scala.swing.Button
import scala.swing.FlowPanel
import scala.swing.Frame
import scala.swing.Panel

/** Defines a trait representing the Swing control panel
  */
trait SwingControlPanel:
  /** Updates the displayed river monitor state.
    *
    * @param riverMonitorData
    *   The representation of the river monitor resource data.
    */
  def updateRiverMonitorData(riverMonitorData: RiverMonitorData): Unit

/** Factory object for creating a SwingControlPanel instance. */
object SwingControlPanel:
  /** Creates a SwingControlPanel instance.
    *
    * @param view
    *   The View associated with the control panel.
    * @return
    *   An instance of SwingControlPanel.
    */
  def apply(view: View): SwingControlPanel = SwingControlPanelImpl(view)

  /** Implementation class for the SwingControlPanel trait. */
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

    // Handle window closing event
    addWindowListener(new WindowAdapter() {
      override def windowClosing(ev: WindowEvent): Unit =
        System.exit(-1)

      override def windowClosed(ev: WindowEvent): Unit =
        System.exit(-1)
    })

    // Called by the View to update the river monitor data
    override def updateRiverMonitorData(riverMonitorData: RiverMonitorData): Unit =
      SwingUtilities.invokeLater(() => {
        riverPanel.updateRiverMonitorData(riverMonitorData)

        riverMonitorData.riverMonitorState match
          case RiverMonitorState.Safe => {
            buttonsPanel.buttonEvacuate.visible = false
            buttonsPanel.buttonEvacuated.visible = false
          }
          case RiverMonitorState.Warned => {
            buttonsPanel.buttonEvacuate.visible = true
            buttonsPanel.buttonEvacuated.visible = false
          }
          case RiverMonitorState.Evacuating => {
            buttonsPanel.buttonEvacuate.visible = false
            buttonsPanel.buttonEvacuated.visible = true
          }
          case null =>
        repaint()
      })

  end SwingControlPanelImpl
end SwingControlPanel

/** Defines a sealed class representing the buttons panel for the control panel GUI. */
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

/** Defines a sealed class representing the river panel for displaying river monitor information. */
sealed class RiverPanel(width: Int, height: Int, viewName: String) extends Panel:
  var riverMonitorData: RiverMonitorData = RiverMonitorData(null, 0, None)

  preferredSize = Dimension(width, height)

  override def paint(g: Graphics2D): Unit =
    val g2: Graphics2D = g
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g2.drawRect(0, 0, width - 1, height - 1)
    g2.setColor(java.awt.Color.BLUE)
    riverMonitorData.riverMonitorState match
      case RiverMonitorState.Safe       => g2.setColor(java.awt.Color.GREEN)
      case RiverMonitorState.Evacuating => g2.setColor(java.awt.Color.YELLOW)
      case RiverMonitorState.Warned     => g2.setColor(java.awt.Color.RED)
      case null                         => g2.setColor(java.awt.Color.CYAN)
    g2.fillRect(0, 0, width, height)
    g2.setColor(java.awt.Color.BLACK)
    var yStringPosition: Int = 15
    g2.drawString(s"View name: $viewName", 5, yStringPosition)
    yStringPosition += 15
    g2.drawString(
      s"River monitor state: ${riverMonitorData.riverMonitorState}",
      5,
      yStringPosition
    )
    yStringPosition += 15
    g2.drawString(
      s"Water level threshold: ${riverMonitorData.threshold}",
      5,
      yStringPosition
    )
    yStringPosition += 15
    g2.drawRect(0, 0, width, height)
    g2.setColor(java.awt.Color.BLACK)
    riverMonitorData.monitoredSensors
      .getOrElse(Map())
      .foreach((sensorName, sensorData) =>
        // var sensorString: String = sensorName
        val sensorString: String =
          s"$sensorName - ${sensorData.map((key, value) => s"$key: $value").mkString(", ")}"
        g2.drawString(sensorString, 5, yStringPosition)
        yStringPosition += 15
      )
  end paint

  /** Updates the displayed river monitor state.
    *
    * @param riverMonitorData
    *   The updated river monitor data.
    */
  def updateRiverMonitorData(riverMonitorData: RiverMonitorData): Unit =
    this.riverMonitorData = riverMonitorData

end RiverPanel
