package com.alekslitvinenk.ecso.actors.charge

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.alekslitvinenk.ecso.config.EcsoConfig
import com.alekslitvinenk.ecso.domain.Protocol.{FinishChargingSession, FinishChargingSessionResponse, StartChargingSession, StartChargingSessionResponse}
import com.alekslitvinenk.ecso.domain.Statuses
import com.alekslitvinenk.ecso.service.HttpClient

/**
 * ChargingStationActor represents charging station which may have 1..n charging terminals
 * It acts as proxy in a sense, that when it receives StartSession or FinishSession messages it redirects them to
 * underlying charging terminal according it terminalId from message
 */

object ChargingStationActor {
  def apply(stationId: Long, httpClient: HttpClient, config: EcsoConfig): Behavior[Command] =
    Behaviors.setup(context => new ChargingStationActor(context, stationId, httpClient, config: EcsoConfig))
}

class ChargingStationActor(context: ActorContext[Command],
                           val stationId: Long,
                           httpClient: HttpClient,
                           config: EcsoConfig) extends AbstractBehavior[Command](context) {
  
  private val log = context.log
  
  private val terminalById: Map[Long, ActorRef[Command]] =
    Map(
      1L -> context.spawn(ChargingTerminalActor(stationId, 1L, httpClient, config), "terminal1"),
      2L -> context.spawn(ChargingTerminalActor(stationId, 2L, httpClient, config), "terminal2"),
      3L -> context.spawn(ChargingTerminalActor(stationId, 3L, httpClient, config), "terminal3"),
    )
  
  override def onMessage(msg: Command): Behavior[Command] = {
    log.debug(s"Inbound message: $msg")
    
    msg match {
      // Matches startSession messages for this particular charging station
      case startSession @ StartSession(StartChargingSession(_, `stationId`, terminalId), replyTo, requestId) =>
        terminalById.get(terminalId) match {
          case Some(terminalRef) =>
            terminalRef ! startSession
            
          case None => replyTo ! StartSessionResponse(StartChargingSessionResponse(Statuses.WrongTerminal), requestId)
        }
        Behaviors.same
  
      // Matches  startSession messages for other charging station
      case StartSession(StartChargingSession(_, _, _), replyTo, requestId) =>
        replyTo ! StartSessionResponse(StartChargingSessionResponse(Statuses.WrongStation), requestId)
        Behaviors.same

      // Matches finishSession messages for this particular charging station
      case finishSession @ FinishSession(FinishChargingSession(_, `stationId`, terminalId), replyTo, requestId) =>
        terminalById.get(terminalId) match {
          case Some(terminalRef) =>
            terminalRef ! finishSession
            
          case None => replyTo ! FinishSessionResponse(
            FinishChargingSessionResponse(Statuses.WrongTerminal),
            requestId,
          )
        }
        Behaviors.same

      // Matches  finishSession messages for other charging station
      case FinishSession(FinishChargingSession(_, _, _), replyTo, requestId) =>
        replyTo ! FinishSessionResponse(FinishChargingSessionResponse(Statuses.WrongStation), requestId)
        Behaviors.same

      case _ =>
        log.error("Response msg received whereas we're not supposed to get it here")
        Behaviors.same
    }
  }
}
