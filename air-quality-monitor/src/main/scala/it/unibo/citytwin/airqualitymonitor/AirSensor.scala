package it.unibo.citytwin.airqualitymonitor

import it.unibo.citytwin.core.model.Point2D

/** Represents an air quality sensor with a name and position.
  * @param name
  *   Name of the sensor
  * @param position
  *   Position of the sensor
  */
case class AirSensor(name: String, position: Point2D[Int])

/** Represents the resource state of the air quality sensor, including PM10, PM2.5, and NOx values.
  * @param pm10
  *   PM10 value
  * @param pm25
  *   PM2.5 value
  * @param nox
  *   NOx value
  */
case class AirSensorResourceState(pm10: Float, pm25: Float, nox: Float)
