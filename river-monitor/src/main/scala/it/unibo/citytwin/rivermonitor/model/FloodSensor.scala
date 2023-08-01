package it.unibo.citytwin.rivermonitor.model

case class FloodSensor(zoneId: Int,
                       position: Point2D,
                       threshold: Float)