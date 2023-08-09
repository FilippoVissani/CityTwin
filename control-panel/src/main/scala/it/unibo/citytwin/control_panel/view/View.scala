package it.unibo.citytwin.control_panel.view

import com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener
import it.unibo.citytwin.control_panel.view.View.MainPanel
import it.unibo.citytwin.core.model.Resource
import java.awt.{Dimension, RenderingHints, Toolkit}
import java.awt.event.{WindowAdapter, WindowEvent}
import javax.swing.SwingUtilities
import scala.swing.{BorderPanel, Frame, Graphics2D, Panel}

trait View:
  def drawResources(resources: Set[Resource]): Unit

object View:
  def apply(): View = ViewImpl()

  private class ViewImpl extends Frame with View:
    private val percentSize = 80
    private val dimension = Dimension(
      percentSize * Toolkit.getDefaultToolkit.getScreenSize.width / 100,
      percentSize * Toolkit.getDefaultToolkit.getScreenSize.height / 100
    )
    private val mainPanel: MainPanel = MainPanel(dimension.width, dimension.height)
    title = "CityTwin Control Panel"
    size = dimension
    resizable = false
    visible = true
    contents = mainPanel

    addWindowListener(new WindowAdapter() {
      override def windowClosing(ev: WindowEvent): Unit =
        System.exit(-1)

      override def windowClosed(ev: WindowEvent): Unit =
        System.exit(-1)
    })
    override def drawResources(resources: Set[Resource]): Unit =
      SwingUtilities.invokeLater(() => {
        mainPanel.refresh(resources)
      })

  end ViewImpl

  sealed class MainPanel(width: Int, height: Int) extends Panel:
    var resources: Set[Resource] = Set()
    preferredSize = Dimension(width, height)

    def refresh(resources: Set[Resource]): Unit =
      this.resources = resources

    override def paint(g: Graphics2D): Unit =
      val g2: Graphics2D = g
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
      g2.setColor(java.awt.Color.WHITE)
      g2.fillRect(0, 0, width - 1, height - 1)
      g2.setColor(java.awt.Color.BLACK)
      resources.filter(r => r.position.isDefined).foreach(r => {
        g2.fillOval(r.position.get.x, r.position.get.y, 10, 10)
      })
    end paint
  end MainPanel
