package it.unibo.citytwin.control_panel.view

import akka.actor.typed.ActorRef
import com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener
import it.unibo.citytwin.control_panel.view.View.{InfoPanel, MapPanel}
import it.unibo.citytwin.core.actors.MainstayActorCommand
import it.unibo.citytwin.core.model.Resource
import java.awt.{Component, Dimension, RenderingHints, Toolkit}
import java.awt.event.{WindowAdapter, WindowEvent}
import java.io.File
import javax.imageio.ImageIO
import javax.swing.{JPanel, JTabbedPane, SwingUtilities}
import scala.io.{BufferedSource, Source}
import scala.swing.TabbedPane.Page
import scala.swing.{BorderPanel, BoxPanel, BufferWrapper, Button, FlowPanel, Frame, Graphics2D, Orientation, Panel, ScrollPane, TabbedPane, TextArea}

trait View:
  def drawResources(resources: Set[Resource]): Unit

  def drawMainstays(mainstays: Set[String]): Unit

object View:
  def apply(): View = ViewImpl()

  private class ViewImpl extends Frame with View:
    private val framePercentSize = (80, 80)
    private val mapPanelPercentSize = (100, 100)
    private val frameDimension = calcFrameDimension(framePercentSize)
    private val mapPanelDimension = calcPanelDimension(mapPanelPercentSize, frameDimension)
    private val mapPanel: MapPanel = MapPanel(frameDimension, mapPanelDimension)
    private val infoPanel = InfoPanel()
    private val mainPane = TabbedPane()
    mainPane.pages += Page("Map", mapPanel)
    mainPane.pages += Page("Info", infoPanel.mainPanel)
    title = "CityTwin Control Panel"
    size = calcFrameDimension(framePercentSize)
    resizable = false
    visible = true
    contents = mainPane

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
        mapPanel.drawResources(resources)
        infoPanel.drawResources(resources)
      })

    override def drawMainstays(mainstays: Set[String]): Unit =
      SwingUtilities.invokeLater(() => {
        infoPanel.drawMainstays(mainstays)
      })

  end ViewImpl

  sealed class MapPanel(frameDimension: Dimension, mapPanelDimension: Dimension) extends Panel:
    private val image = Toolkit.getDefaultToolkit.getImage("control-panel/src/main/resources/city-map.png")
    private var resources: Set[Resource] = Set()
    preferredSize = frameDimension

    def drawResources(resources: Set[Resource]): Unit =
      this.resources = resources

    override def paint(g: Graphics2D): Unit =
      val g2: Graphics2D = g
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
      // RENDER RESOURCES STATE
      g2.drawImage(image, 0, 0, mapPanelDimension.width, mapPanelDimension.height, null)
      g2.setColor(java.awt.Color.RED)
      resources.filter(r => r.position.isDefined).foreach(r => g2.fillOval(r.position.get.x, r.position.get.y, 10, 10))
    end paint
  end MapPanel

  sealed class InfoPanel:
    private val mainstaysInfoTextArea = new TextArea {
      this.text = "MAINSTAYS INFO:\n"
      this.editable = false
    }
    private val mainstaysInfoPanel = ScrollPane(mainstaysInfoTextArea)
    private val resourcesInfoTextArea = new TextArea {
      this.text = "RESOURCES INFO:\n"
      this.editable = false
    }
    private val resourcesInfoPanel = ScrollPane(resourcesInfoTextArea)
    val mainPanel: BoxPanel = new BoxPanel(Orientation.Horizontal) {
      contents ++= Seq(mainstaysInfoPanel, resourcesInfoPanel)
    }

    def drawResources(resources: Set[Resource]): Unit =
      resourcesInfoTextArea.text = "RESOURCES INFO:\n"
      resources.foreach(r => resourcesInfoTextArea.text = resourcesInfoTextArea.text + r.toString + "\n")

    def drawMainstays(mainstays: Set[String]): Unit =
      mainstaysInfoTextArea.text = "MAINSTAYS INFO:\n"
      mainstays.foreach(m => mainstaysInfoTextArea.text = mainstaysInfoTextArea.text + m + "\n")