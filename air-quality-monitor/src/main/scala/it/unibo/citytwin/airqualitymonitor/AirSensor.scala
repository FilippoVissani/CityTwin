package it.unibo.citytwin.airqualitymonitor

import it.unibo.citytwin.core.model.Point2D
import upickle.default.ReadWriter

/** Represents an air quality sensor with a name and position.
  * @param name
  *   Name of the sensor
  * @param position
  *   Position of the sensor
  */
case class AirSensor(name: String, position: Point2D[Int])

/** Represents the data of the air quality sensor, including PM10, PM2.5, and NOx values.
  * @param pm10
  *   PM10 value
  * @param pm25
  *   PM2.5 value
  * @param nox
  *   NOx value
  */
case class AirSensorData(pm10: Float, pm25: Float, nox: Float) derives ReadWriter
