package com.alekslitvinenk.ecso.actors.charge

import java.time.Instant
import java.util.UUID

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.alekslitvinenk.ecso.actors.charge.ChargingTerminalActor.AsyncBillSessionResultWrap
import com.alekslitvinenk.ecso.config.EcsoConfig
import com.alekslitvinenk.ecso.domain.Protocol.{ActiveChargingSession, BillSessionResponse, FinishChargingSession, FinishChargingSessionResponse, FinishedChargingSession, SessionSettlement, StartChargingSession, StartChargingSessionResponse, Status}
import com.alekslitvinenk.ecso.domain.Statuses
import com.alekslitvinenk.ecso.service.HttpClient

/**
 * ChargingTerminalActor represents charging terminal and this is a locked resource which can handle on charging
 * session at a time
 */

import scala.util.{Failure, Success}

object ChargingTerminalActor {
  def apply(stationId: Long, terminalId: Long, httpClient: HttpClient, config: EcsoConfig): Behavior[Command] =
    Behaviors.setup(context => new ChargingTerminalActor(context, stationId, terminalId, httpClient, config))
  
  final case class AsyncBillSessionResultWrap(
    status: Status,
    settlement: Option[SessionSettlement],
    requestId: Long) extends Command
}

/**
 * ChargingTerminalActor represents real physical device that is capable of charging EV
 * This device can be uniquely identified by the pair of its charging station ID and the device ID itself
 *
 * @param context - ActorContext
 * @param stationId - Charging station ID
 * @param terminalId - Charging device (terminal) ID that is unique withing its charging station [[ChargingStationActor]]
 */
class ChargingTerminalActor(context: ActorContext[Command],
                            val stationId: Long,
                            val terminalId: Long,
                            httpClient: HttpClient,
                            config: EcsoConfig)
  extends AbstractBehavior[Command](context) {
  
  private var activeSessionOpt: Option[ActiveChargingSession] = None
  private var recipientByRequestId: Map[Long, ActorRef[Command]] = Map.empty
  private val log = context.log
  
  import akka.actor.typed.scaladsl.adapter._
  private implicit val system: ActorSystem = context.system.toClassic
  
  private def startNewActiveChargingSession(driverId: Long): ActiveChargingSession =
    ActiveChargingSession(
      driverId = driverId,
      startTime = Instant.now(),
      sessionTicket = UUID.randomUUID().toString
    )
  
  private def finishActiveChargingSession(activeChargingSession: ActiveChargingSession): FinishedChargingSession = {
    
    val finishTime = Instant.now()
    val durationHours = (finishTime.getEpochSecond - activeChargingSession.startTime.getEpochSecond) / 3600D
  
      FinishedChargingSession(
        driverId = activeChargingSession.driverId,
        startTime = activeChargingSession.startTime,
        sessionTicket = activeChargingSession.sessionTicket,
        finishTime = Instant.now(),
        consumedEnergy = config.defaultEnergyConsumption * durationHours
      )
  }
  
  override def onMessage(msg: Command): Behavior[Command] = {
    log.debug(s"Inbound message: $msg")
  
    msg match {
      case StartSession(StartChargingSession(driverId, `stationId`, `terminalId`), replyTo, requestId) =>
        activeSessionOpt match {
          case None =>
            val session = startNewActiveChargingSession(driverId)
            activeSessionOpt = Some(session)
            replyTo ! StartSessionResponse(
              StartChargingSessionResponse(Statuses.Ok, Some(session.sessionTicket)),
              requestId
            )
            Behaviors.same
        
          case Some(_) =>
            replyTo ! StartSessionResponse(
              StartChargingSessionResponse(Statuses.TerminalAlreadyHasActiveSession),
              requestId
            )
            
            Behaviors.same
        }
    
      case StartSession(StartChargingSession(_, _, _), replyTo, requestId) =>
        replyTo ! StartSessionResponse(StartChargingSessionResponse(Statuses.WrongTerminal), requestId)
        Behaviors.same
    
      case FinishSession(FinishChargingSession(driverId, `stationId`, `terminalId`), replyTo, requestId) =>
        activeSessionOpt match {
          case None =>
            replyTo ! FinishSessionResponse(FinishChargingSessionResponse(Statuses.IdleTerminal), requestId)
        
          case Some(sessionRef @ ActiveChargingSession(_, _, _)) =>
            if (sessionRef.driverId == driverId) {
              activeSessionOpt = None
              val finishedChargingSession = finishActiveChargingSession(sessionRef)
              recipientByRequestId += requestId -> replyTo
              
              val futureRes = httpClient.requestSessionSettlement(finishedChargingSession, requestId)
              
              context.pipeToSelf(futureRes) {
                case Success(BillSessionResponse(status, maybeSessionSettlement, requestId)) =>
                  AsyncBillSessionResultWrap(status, maybeSessionSettlement, requestId)

                case Failure(exception) =>
                  log.error(exception.getMessage)
                  AsyncBillSessionResultWrap(Statuses.UnknownError, None, requestId)
              }
              
            } else {
              replyTo ! FinishSessionResponse(FinishChargingSessionResponse(Statuses.WrongDriverId), requestId)
            }
        }
        Behaviors.same
    
      case FinishSession(_, replyTo, requestId) =>
        replyTo ! FinishSessionResponse(FinishChargingSessionResponse(Statuses.WrongTerminal), requestId)
        Behaviors.same

      case AsyncBillSessionResultWrap(status, maybeSessionSettlement, requestId) =>
        recipientByRequestId.get(requestId).fold
        {
          log.error("Recipient not found")
        } { replayTo =>
          replayTo ! FinishSessionResponse(
            FinishChargingSessionResponse(status, maybeSessionSettlement), requestId
          )
        }
        Behaviors.same
    }
  }
}
