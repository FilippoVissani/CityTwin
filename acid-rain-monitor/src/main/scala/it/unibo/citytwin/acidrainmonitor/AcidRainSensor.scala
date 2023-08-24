package it.unibo.citytwin.acidrainmonitor

import it.unibo.citytwin.core.model.Point2D

/** Represents an acid rain sensor with a name and position.
  * @param name
  *   Name of the sensor
  * @param position
  *   Position of the sensor
  */
case class AcidRainSensor(name: String, position: Point2D[Int])
