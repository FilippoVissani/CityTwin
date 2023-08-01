package it.unibo.citytwin.rivermonitor.model

case class Boundary(x0: Int,
                    y0: Int,
                    x1: Int,
                    y1: Int):
  def width: Int = x1 - x0
  def height: Int = y1 - y0
