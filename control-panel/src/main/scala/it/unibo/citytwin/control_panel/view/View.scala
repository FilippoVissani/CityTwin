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
import scala.swing.{
  BorderPanel,
  BoxPanel,
  BufferWrapper,
  Button,
  FlowPanel,
  Frame,
  Graphics2D,
  Orientation,
  Panel,
  ScrollPane,
  TabbedPane,
  TextArea
}

trait View:
  def drawResources(resources: Set[Resource]): Unit

  def drawMainstays(mainstays: Set[String]): Unit

object View:
  def apply(citySize: (Double, Double)): View = ViewImpl(citySize)

  private class ViewImpl(citySize: (Double, Double)) extends Frame with View:
    private val framePercentSize    = (80, 80)
    private val mapPanelPercentSize = (100, 100)
    private val frameDimension      = calcFrameDimension(framePercentSize)
    private val mapPanelDimension   = calcPanelDimension(mapPanelPercentSize, frameDimension)
    private val mapPanel: MapPanel  = MapPanel(frameDimension, mapPanelDimension, citySize)
    private val infoPanel           = InfoPanel()
    private val mainPane            = TabbedPane()
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

  sealed class MapPanel(frameDimension: Dimension, mapPanelDimension: Dimension, citySize: (Double, Double)) extends Panel:
    private val image =
      Toolkit.getDefaultToolkit.getImage("control-panel/src/main/resources/city-map.png")
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
      resources
        .filter(r => r.position.isDefined)
        .filter(r => r.name.isDefined)
        .filter(r => r.nodeState.isDefined)
        .foreach(r => {
          val scaledPosition = (scaleX(r.position.get.x), scaleY(r.position.get.y))
          g2.setColor(java.awt.Color.BLACK)
          g2.drawString(r.name.get, scaledPosition._1 - 20, scaledPosition._2 - 10)
          if r.nodeState.get then
            g2.setColor(java.awt.Color.RED)
          else
            g2.setColor(java.awt.Color.GRAY)
          g2.fillOval(scaledPosition._1, scaledPosition._2, 10, 10)
        })
    end paint

    private def scaleX(value: Double): Int =
      val percent = value * 100 / citySize._1
      Math.round(percent * size.width / 100).toInt

    private def scaleY(value: Double): Int =
      val percent = value * 100 / citySize._2
      Math.round(percent * size.height / 100).toInt

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
      val divider = "#################################################### \n"
      resources.toList.sortBy(_.name.getOrElse("")).foreach(r =>
        resourcesInfoTextArea.text = resourcesInfoTextArea.text + divider
        resourcesInfoTextArea.text = resourcesInfoTextArea.text + formatResource(r) + "\n"
        resourcesInfoTextArea.text = resourcesInfoTextArea.text + divider
      )

    def drawMainstays(mainstays: Set[String]): Unit =
      mainstaysInfoTextArea.text = "MAINSTAYS INFO:\n"
      mainstays.foreach(m => mainstaysInfoTextArea.text = mainstaysInfoTextArea.text + m + "\n")

    private def formatResource(resource: Resource): String =
      var result: String = ""
      if resource.nodeState.isDefined then
        result = result + s"Node State: ${resource.nodeState.get} \n"
      if resource.name.isDefined then
        result = result + s"Name: ${resource.name.get} \n"
      if resource.position.isDefined then
        result = result + s"Position: ${resource.position.get} \n"
      if resource.state.isDefined then
        result = result + s"State: ${resource.state.get} \n"
      result = result + s"Resource type: "
      resource.resourceType.foreach(t => result = result + t + " ")
      result
