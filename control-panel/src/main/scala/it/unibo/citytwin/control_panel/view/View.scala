package it.unibo.citytwin.control_panel.view

import akka.actor.typed.ActorRef
import com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener
import it.unibo.citytwin.control_panel.view.View.MainPanel
import it.unibo.citytwin.core.actors.MainstayActorCommand
import it.unibo.citytwin.core.model.Resource

import java.awt.{Dimension, RenderingHints, Toolkit}
import java.awt.event.{WindowAdapter, WindowEvent}
import javax.swing.SwingUtilities
import scala.swing.{BorderPanel, Frame, Graphics2D, Panel}

trait View:
  def drawResources(resources: Set[Resource]): Unit

  def drawMainstays(mainstays: Set[String]): Unit

object View:
  def apply(): View = ViewImpl()

  private class ViewImpl extends Frame with View:
    private val framePercentSize = (80, 80)
    private val resourcesPanelPercentSize = (80, 100)
    private val mainstaysPanelPercentSize = (20, 100)
    private val frameDimension = calcFrameDimension(framePercentSize)
    private val resourcesPanelDimension = calcPanelDimension(resourcesPanelPercentSize, frameDimension)
    private val mainstaysPanelDimension = calcPanelDimension(mainstaysPanelPercentSize, frameDimension)
    private val mainPanel: MainPanel = MainPanel(frameDimension, resourcesPanelDimension, mainstaysPanelDimension)
    title = "CityTwin Control Panel"
    size = calcFrameDimension(framePercentSize)
    resizable = false
    visible = true
    contents = mainPanel

    private def calcFrameDimension(framePercentSize: (Int, Int)) =
      Dimension(
        framePercentSize._1 * Toolkit.getDefaultToolkit.getScreenSize.width / 100,
        framePercentSize._2 * Toolkit.getDefaultToolkit.getScreenSize.height / 100
      )

    private def calcPanelDimension(panelPercentSize: (Int, Int), frameDimension: Dimension) =
      Dimension(
        panelPercentSize._1 * frameDimension.width / 100,
        panelPercentSize._2 * frameDimension.height / 100
      )

    addWindowListener(new WindowAdapter() {
      override def windowClosing(ev: WindowEvent): Unit =
        System.exit(-1)

      override def windowClosed(ev: WindowEvent): Unit =
        System.exit(-1)
    })

    override def drawResources(resources: Set[Resource]): Unit =
      SwingUtilities.invokeLater(() => {
        mainPanel.drawResources(resources)
      })

    override def drawMainstays(mainstays: Set[String]): Unit =
      SwingUtilities.invokeLater(() => {
        mainPanel.drawMainstays(mainstays)
      })

  end ViewImpl

  sealed class MainPanel(frameDimension: Dimension, resourcesPanelDimension: Dimension, mainstaysPanelDimension: Dimension) extends Panel:
    private var resources: Set[Resource] = Set()
    private var mainstays: Set[String] = Set()
    private val mainstaysPanelArea: (Int, Int, Int, Int) = (0, 0, mainstaysPanelDimension.width - 1, mainstaysPanelDimension.height - 1)
    private val resourcesPanelArea: (Int, Int, Int, Int) = (mainstaysPanelDimension.width, mainstaysPanelDimension.height, resourcesPanelDimension.width - 1, resourcesPanelDimension.height - 1)
    preferredSize = frameDimension

    def drawResources(resources: Set[Resource]): Unit =
      this.resources = resources

    def drawMainstays(mainstays: Set[String]): Unit =
      this.mainstays = mainstays

    override def paint(g: Graphics2D): Unit =
      val g2: Graphics2D = g
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
      // RENDER MAINSTAYS STATE
      g2.setColor(java.awt.Color.BLUE)
      g2.fillRect(mainstaysPanelArea._1, mainstaysPanelArea._2, mainstaysPanelArea._3, mainstaysPanelArea._4)
      g2.setColor(java.awt.Color.WHITE)
      mainstays.foreach(m => g2.drawString(m.toString, mainstaysPanelArea._1 + 5, mainstaysPanelArea._2 + 15))
      // RENDER RESOURCES STATE
      g2.setColor(java.awt.Color.WHITE)
      g2.fillRect(resourcesPanelArea._1, resourcesPanelArea._2, resourcesPanelArea._3, resourcesPanelArea._4)
      g2.setColor(java.awt.Color.BLACK)
      resources.filter(r => r.position.isDefined).foreach(r => g2.fillOval(r.position.get.x, r.position.get.y, 10, 10))
    end paint
  end MainPanel
