package it.unibo.citytwin.rivermonitor.model

import it.unibo.citytwin.core.model.Point2D

/** A flood sensor
  *
  * @param name
  *   the name of the flood sensor
  * @param position
  *   the position of the flood sensor
  */
case class FloodSensor(name: String, position: Point2D[Int])
