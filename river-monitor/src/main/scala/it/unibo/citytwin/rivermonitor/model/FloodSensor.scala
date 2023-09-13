package it.unibo.citytwin.rivermonitor.model

import it.unibo.citytwin.core.model.Point2D
import upickle.default.ReadWriter

/** A flood sensor
  *
  * @param name
  *   the name of the flood sensor
  * @param position
  *   the position of the flood sensor
  */
case class FloodSensor(name: String, position: Point2D[Int])

/** The data of a flood sensor
  *
  * @param waterLevel
  *   the water level measured by the flood sensor
  */
case class FloodSensorData(waterLevel: Float) derives ReadWriter
