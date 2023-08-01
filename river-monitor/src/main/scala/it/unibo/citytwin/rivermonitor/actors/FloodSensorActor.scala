package it.unibo.citytwin.rivermonitor.actors
import akka.actor
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import it.unibo.citytwin.rivermonitor.model.FloodSensor
import akka.util.Timeout
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import it.unibo.citytwin.rivermonitor.actors.IsAlarmResult.*
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import scala.util.{Failure, Success}

import concurrent.duration.DurationInt
import java.util.concurrent.ThreadLocalRandom

object IsAlarmResult extends Enumeration:
  type IsAlarmResult = Value
  val Alarm, NotAlarm, Unreachable = Value

trait FloodSensorActorCommand
object Tick extends Serializable with FloodSensorActorCommand
case class AskIsAlarm(replyTo: ActorRef[FloodSensorActorCommand]) extends Serializable with FloodSensorActorCommand
case class TellIsAlarm(isAlarm: IsAlarmResult) extends Serializable with FloodSensorActorCommand
case class IsMyZoneRequestFloodSensor(zoneId: Int, replyTo: ActorRef[FloodSensorActorCommand]) extends Serializable with FloodSensorActorCommand
case class IsMyZoneResponseFromRiverMonitorToFloodSensor(replyTo: ActorRef[RiverMonitorActorCommand]) extends Serializable with FloodSensorActorCommand
case class IsMyZoneResponseFromFloodSensorToFloodSensor(replyTo: ActorRef[FloodSensorActorCommand]) extends Serializable with FloodSensorActorCommand

val floodSensorService = ServiceKey[FloodSensorActorCommand]("floodSensorService")

object FloodSensorActor :
  def apply(floodSensor: FloodSensor,
            floodSensorActors: Set[ActorRef[FloodSensorActorCommand]] = Set(),
            alarms: List[IsAlarmResult] = List(),
            riverMonitorActor: Option[ActorRef[RiverMonitorActorCommand]] = Option.empty): Behavior[FloodSensorActorCommand] =
    Behaviors.setup[FloodSensorActorCommand] { ctx =>
      Behaviors.withTimers { timers =>
        ctx.system.receptionist ! Receptionist.register(floodSensorService, ctx.self)
        timers.startTimerAtFixedRate(Tick, 5.seconds)
        FloodSensorActorLogic(ctx, floodSensor, floodSensorActors, alarms, riverMonitorActor)
      }
    }

  def FloodSensorActorLogic(ctx: ActorContext[FloodSensorActorCommand],
                            floodSensor: FloodSensor,
                            floodSensorActors: Set[ActorRef[FloodSensorActorCommand]] = Set(),
                            alarms: List[IsAlarmResult] = List(),
                            riverMonitorActor: Option[ActorRef[RiverMonitorActorCommand]] = Option.empty): Behavior[FloodSensorActorCommand] =
    implicit val timeout: Timeout = 2.seconds
    Behaviors.receiveMessage {
      case IsMyZoneRequestFloodSensor(zoneId, replyTo) => {
        ctx.log.debug(s"Received IsMyZoneRequestFloodSensor")
        if floodSensor.zoneId == zoneId then
          replyTo ! IsMyZoneResponseFromFloodSensorToFloodSensor(ctx.self)
        Behaviors.same
      }
      case IsMyZoneResponseFromRiverMonitorToFloodSensor(replyTo) => {
        ctx.log.debug(s"Received IsMyZoneResponseFromRiverMonitorToFloodSensor")
        FloodSensorActorLogic(ctx, floodSensor, floodSensorActors, alarms, Option(replyTo))
      }
      case IsMyZoneResponseFromFloodSensorToFloodSensor(replyTo) => {
        ctx.log.debug(s"Received IsMyZoneResponseFromFloodSensorToFloodSensor")
        FloodSensorActorLogic(ctx, floodSensor, floodSensorActors + replyTo, alarms, riverMonitorActor)
      }
      case Tick => {
        ctx.log.debug(s"Received Tick")
        floodSensorActors.foreach(actor => {
          ctx.ask(actor, AskIsAlarm.apply) {
            case Success(TellIsAlarm(isAlarm)) => TellIsAlarm(isAlarm)
            case _ => TellIsAlarm(Unreachable)
          }
        })
        FloodSensorActorLogic(ctx, floodSensor, floodSensorActors, List(), riverMonitorActor)
      }
      case AskIsAlarm(replyTo) => {
        ctx.log.debug(s"Received RequestIsAlarm")
        if ThreadLocalRandom.current().nextBoolean() then
          if ThreadLocalRandom.current().nextFloat(20) > floodSensor.threshold then
            replyTo ! TellIsAlarm(Alarm)
          else
            replyTo ! TellIsAlarm(NotAlarm)
        Behaviors.same
      }
      case TellIsAlarm(isAlarm) => {
        ctx.log.debug(s"Received IsAlarmResponse")
        val tmpAlarms = isAlarm :: alarms
        if tmpAlarms.size == floodSensorActors.size then
          if tmpAlarms.count(state => state == Alarm) > tmpAlarms.size / 2 && riverMonitorActor.isDefined then
            riverMonitorActor.get ! WarnRiverMonitor
          Behaviors.same
        else
          FloodSensorActorLogic(ctx, floodSensor, floodSensorActors, tmpAlarms, riverMonitorActor)
      }
      case _ => Behaviors.stopped
    }