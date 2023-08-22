package it.unibo.citytwin.rivermonitor.app

import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.core.model.Point2D
import it.unibo.citytwin.rivermonitor.actors.floodsensor.FloodSensorActor
import it.unibo.citytwin.rivermonitor.model.FloodSensor

@main def floodSensorMain(args: String*): Unit =
  if args.length < 4 then
    println("Usage: floodSensorMain <Port> <floodSensorName> <x_position> <y_position>")
  else
    val port            = args(0).toInt
    val floodSensorName = args(1)
    val positionX       = args(2).toInt
    val positionY       = args(3).toInt
    val floodSensor     = FloodSensor(floodSensorName, Point2D[Int](positionX, positionY))
    startup(port)(FloodSensorActor(floodSensor))
