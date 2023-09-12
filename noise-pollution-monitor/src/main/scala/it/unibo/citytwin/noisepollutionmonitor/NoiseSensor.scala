package it.unibo.citytwin.noisepollutionmonitor

import it.unibo.citytwin.core.model.Point2D
import upickle.default.ReadWriter

/** Represents an noise sensor with a name and position.
  * @param name
  *   Name of the sensor
  * @param position
  *   Position of the sensor
  */
case class NoiseSensor(name: String, position: Point2D[Int])

/** Represents the data of a noise sensor.
  * @param value
  *   Value in dB measured by the sensor
  * @param description
  *   Description of the noise
  */
case class NoiseSensorData(value: Int, description: String) derives ReadWriter
