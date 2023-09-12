package it.unibo.citytwin.acidrainmonitor

import it.unibo.citytwin.core.model.Point2D
import upickle.default.ReadWriter

/** Represents an acid rain sensor with a name and position.
  * @param name
  *   Name of the sensor
  * @param position
  *   Position of the sensor
  */
case class AcidRainSensor(name: String, position: Point2D[Int])

/** Represents the data of the acid rain sensor.
  *
  * @param ph
  *   The ph value measured by the acid rain sensor
  */
case class AcidRainSensorData(ph: Float) derives ReadWriter
