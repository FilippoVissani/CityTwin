package it.unibo.citytwin.control_panel.view

import it.unibo.citytwin.core.model.Resource
import scala.swing.Frame

trait View:
  def drawResources(resources: Set[Resource]): Unit

object View:
  def apply(): View = ViewImpl()

  private class ViewImpl extends Frame with View:

    override def drawResources(resources: Set[Resource]): Unit = println(resources)
