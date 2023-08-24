package it.unibo.citytwin.noisepollutionmonitor

import it.unibo.citytwin.core.model.Point2D

/** Represents an noise sensor with a name and position.
  * @param name
  *   Name of the sensor
  * @param position
  *   Position of the sensor
  */
case class NoiseSensor(name: String, position: Point2D[Int])
