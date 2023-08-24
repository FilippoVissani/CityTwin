package it.unibo.citytwin.airqualitymonitor

import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.core.model.Point2D

/** Main entry point of the application. Starts the AirQualityMonitor application with the specified
  * parameters.
  * @param args
  *   Command-line arguments: <Port> <airSensorName> <x_position> <y_position> <sensor_uri>
  */
@main def main(args: String*): Unit =
  if args.length != 5 then {
    println("Usage: main <Port> <airSensorName> <x_position> <y_position> <sensor_uri>")
    println(args(4))
  } else
    // Extract command-line arguments
    val port          = args(0).toInt
    val airSensorName = args(1)
    val positionX     = args(2).toInt
    val positionY     = args(3).toInt
    val sensorUri     = args(4)
    val airSensor     = AirSensor(airSensorName, Point2D[Int](positionX, positionY))
    // Start the actor system
    startup(port)(AirSensorActor(airSensor, sensorUri))
