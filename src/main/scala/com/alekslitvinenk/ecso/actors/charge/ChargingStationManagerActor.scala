package com.alekslitvinenk.ecso.actors.charge

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.alekslitvinenk.ecso.config.EcsoConfig
import com.alekslitvinenk.ecso.domain.Protocol.{FinishChargingSession, FinishChargingSessionResponse, StartChargingSession, StartChargingSessionResponse}
import com.alekslitvinenk.ecso.domain.Statuses
import com.alekslitvinenk.ecso.service.HttpClient

/**
 * ChargingStationManagerActor represents an abstract entity which can be viewed as a group of charging station
 * and is an entry point for all StartSession and FinishSession queries
 */

object ChargingStationManagerActor {
  def apply(httpClient: HttpClient, config: EcsoConfig): Behavior[Command] =
    Behaviors.setup(context => new ChargingStationManagerActor(context, httpClient: HttpClient, config))
}

class ChargingStationManagerActor(context: ActorContext[Command],
                                  httpClient: HttpClient,
                                  config: EcsoConfig) extends AbstractBehavior[Command](context) {
  
  private val log = context.log
  
  private val stationById: Map[Long, ActorRef[Command]] =
    Map(
      1L -> context.spawn(ChargingStationActor(1L, httpClient, config), "station1"),
      2L -> context.spawn(ChargingStationActor(2L, httpClient, config), "station2"),
      3L -> context.spawn(ChargingStationActor(3L, httpClient, config), "station3"),
    )
  
  private var recipientByRequestId: Map[Long, ActorRef[Command]] = Map.empty
  
  override def onMessage(msg: Command): Behavior[Command] = {
    log.debug(s"Inbound message: $msg")
    
    msg match {
      case startSession @ StartSession(StartChargingSession(_, stationId, _), replyTo, requestId) =>
        stationById.get(stationId) match {
          case Some(stationRef) => stationRef ! startSession
          case None => replyTo ! StartSessionResponse(StartChargingSessionResponse(Statuses.WrongStation), requestId)
        }
        Behaviors.same
    
      case finishSession @ FinishSession(FinishChargingSession(_, stationId, _), replyTo, requestId) =>
        stationById.get(stationId) match {
          case Some(stationRef) =>
            recipientByRequestId += requestId -> replyTo
            stationRef ! finishSession
            
          case None => replyTo ! FinishSessionResponse(FinishChargingSessionResponse(Statuses.WrongStation), requestId)
        }
        Behaviors.same

      case _ =>
        log.error("Response msg received whereas we're not supposed to get it here")
        Behaviors.same
    }
  }
}
