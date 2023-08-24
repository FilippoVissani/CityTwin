package it.unibo.citytwin.acidrainmonitor

import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.core.model.Point2D

/** Main entry point of the application. Starts the AcidRainMonitor application with the specified
  * parameters.
  * @param args
  *   Command-line arguments: <Port> <acidRainSensorName> <x_position> <y_position>
  */
@main def main(args: String*): Unit =
  if args.length != 4 then {
    println("Usage: main <Port> <acidRainSensorName> <x_position> <y_position>")
    println(args(4))
  } else
    // Extract command-line arguments
    val port               = args(0).toInt
    val acidRainSensorName = args(1)
    val positionX          = args(2).toInt
    val positionY          = args(3).toInt
    // Create an instance of AcidRainSensor using the provided arguments
    val acidRainSensor = AcidRainSensor(acidRainSensorName, Point2D[Int](positionX, positionY))
    // Start the actor system
    startup(port)(AcidRainSensorActor(acidRainSensor))
