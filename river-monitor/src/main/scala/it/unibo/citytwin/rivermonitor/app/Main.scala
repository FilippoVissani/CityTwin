package it.unibo.citytwin.rivermonitor.app

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import it.unibo.citytwin.core.model.Point2D
import it.unibo.citytwin.rivermonitor.actors.floodsensor.FloodSensorGuardianActor
import it.unibo.citytwin.rivermonitor.actors.ViewGuardianActor
import it.unibo.citytwin.rivermonitor.actors.rivermonitor.RiverMonitorGuardianActor
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.Safe
import it.unibo.citytwin.rivermonitor.model.ZoneState.Ok
import it.unibo.citytwin.rivermonitor.model.{Boundary, FloodSensor, Point2D, RiverMonitor, Zone}

import scala.util.Random

object Main:

/*  def generateZones(rows: Int, columns: Int, zoneSize: Boundary): List[Zone] =
    var zones: List[Zone] = List()
    var k: Int = 0
    for i <- 0 until columns do
      for j <- 0 until rows do
        zones = Zone(k,
          Boundary(i * zoneSize.width, j * zoneSize.height, (i * zoneSize.width) + zoneSize.width - 1, (j * zoneSize.height) + zoneSize.height - 1),
          Ok) :: zones
        k = k + 1
    zones.reverse*/

/*  def generateRiverMonitor(zones: List[Zone]): List[RiverMonitor] =
    var riverMonitors: List[RiverMonitor] = List()
    val random: Random = Random(System.currentTimeMillis())
    zones.foreach(x => riverMonitors = RiverMonitor(x.id,
      Point2D(random.nextInt(x.bounds.width - 100) + x.bounds.x0 + 50, random.nextInt(x.bounds.height - 100) + x.bounds.y0 + 50),
      Safe) :: riverMonitors)
    riverMonitors.reverse*/

  @main def main(): Unit =
    val floodSensorName = "floodSensor1"
    val floodSensor = FloodSensor(floodSensorName, Point2D[Int](0, 0))
    startup(port = 2551)(FloodSensorGuardianActor(floodSensor))

    val riverMonitorName ="riverMonitor1"
    val sensorsToCheck = Set[String]("floodSensor1")
    val riverMonitor = RiverMonitor(riverMonitorName, sensorsToCheck, Point2D[Int](0, 0))
    startup(port = 2552)(FloodSensorGuardianActor(floodSensor))

    /*val rows = 1
    val columns = 2
    val width = columns * 200
    val height = rows * 200
    var port: Int = 2551
    val zones: List[Zone] = generateZones(rows, columns, Boundary(0, 0, width / columns, height / rows))
    val riverMonitors: List[RiverMonitor] = generateRiverMonitor(zones)
    val floodSensors: List[FloodSensor] = generateFloodSensors(zones)
    riverMonitors.foreach(f => {
      startup(port = port)(RiverMonitorGuardianActor(f, zones(f.zoneId)))
      port = port + 1
    })
    floodSensors.foreach(p => {
      startup(port = port)(FloodSensorGuardianActor(p))
      port = port + 1
    })
    startup(port = 1200)(ViewGuardianActor(0, width, height))
    startup(port = 1201)(ViewGuardianActor(1, width, height))*/

  def startup[X](file: String = "cluster", port: Int)(root: => Behavior[X]): ActorSystem[X] =
    // Override the configuration of the port
    val config: Config = ConfigFactory
      .parseString(s"""akka.remote.artery.canonical.port=$port""")
      .withFallback(ConfigFactory.load(file))
    // Create an Akka system
    ActorSystem(root, "ClusterSystem", config)
