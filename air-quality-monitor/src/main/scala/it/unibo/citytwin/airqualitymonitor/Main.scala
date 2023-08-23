package it.unibo.citytwin.airqualitymonitor

import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.core.model.Point2D

/** Main entry point of the application. Starts the AirQualityMonitor application with the specified
  * parameters.
  * @param args
  *   Command-line arguments: <Port> <airSensorName> <x_position> <y_position>
  */
@main def main(args: String*): Unit =
  if args.length != 4 then println("Usage: main <Port> <airSensorName> <x_position> <y_position>")
  else
    val port          = args(0).toInt
    val airSensorName = args(1)
    val positionX     = args(2).toInt
    val positionY     = args(3).toInt
    val airSensor     = AirSensor(airSensorName, Point2D[Int](positionX, positionY))
    startup(port)(AirSensorActor(airSensor))
