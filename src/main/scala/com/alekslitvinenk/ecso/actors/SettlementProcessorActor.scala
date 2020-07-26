package com.alekslitvinenk.ecso.actors

import java.time.Instant

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import com.alekslitvinenk.ecso.actors.SettlementProcessorActor._
import com.alekslitvinenk.ecso.config.EcsoConfig
import com.alekslitvinenk.ecso.domain.Protocol.{FinishedChargingSession, SessionSettlement, SessionTotals, Status, TariffPlan}
import com.alekslitvinenk.ecso.domain.Statuses
import com.alekslitvinenk.ecso.service.SessionStore
import com.alekslitvinenk.ecso.util.CurrencyFormatUtils._

import scala.collection.SortedSet
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object SettlementProcessorActor {
  
  def apply(storeService: SessionStore, config: EcsoConfig)(implicit ecBlocking: ExecutionContext): Behavior[Command] = {
    val baseBehavior =
      Behaviors.setup[Command](context => new SettlementProcessorActor(context, storeService: SessionStore, config))
    
    Behaviors.supervise(baseBehavior).onFailure(SupervisorStrategy.restart)
  }
  
  trait Command
  
  final case class ProcessFinishedSession(
    session: FinishedChargingSession,
    replayTo: ActorRef[Command],
    requestId: Long) extends Command
  
  final case class ProcessFinishedSessionResponse(
    status: Status,
    requestId: Long,
    settlement: Option[SessionSettlement] = None) extends Command
  
  final case class AsyncSessionSettlementResultWrap(settlement: Option[SessionSettlement], requestId: Long) extends Command
  
  final case class SubmitTariffPlan(tariffPlan: TariffPlan, replayTo: ActorRef[Command]) extends Command
  final case class SubmitTariffPlanResponse(status: Status) extends Command
}

class SettlementProcessorActor(context     : ActorContext[Command],
                               storeService: SessionStore,
                               config      : EcsoConfig)(implicit ecBlocking: ExecutionContext) extends AbstractBehavior[Command](context) {
  
  private var tariffPlans: SortedSet[TariffPlan] = SortedSet.empty
  
  if (config.useDefaultTariffPlan) {
    val defaultTariff = config.defaultTariffPlan
    
    tariffPlans += TariffPlan(
      // Default tariff should be valid from some moment in past so that we can settle session that starts and ends
      // at the application start time
      activationTime = Instant.now().minusSeconds(3600),
      energyConsumptionFee = defaultTariff.energyConsumptionFee,
      parkingFee = defaultTariff.parkingFee,
      serviceFee = defaultTariff.serviceFee,
    )
  }
  
  private var recipientByRequestId: Map[Long, ActorRef[Command]] = Map.empty
  
  private val log = context.log
  
  private def findApplicableTariffPlanForSession(session: FinishedChargingSession): Option[TariffPlan] =
    tariffPlans.find(t => session.startTime.isAfter(t.activationTime))
  
  private def getSessionDurationHours(finishedChargingSession: FinishedChargingSession): Long = {
    val startTime = finishedChargingSession.startTime
    val finishTime = finishedChargingSession.finishTime
    Math.ceil(finishTime.minusSeconds(startTime.getEpochSecond).getEpochSecond.toDouble / 3600D).toLong
  }
  
  /**
   * Converts given tariff to the tariff with 2 decimal digits
   * @param tariff - Raw (unrounded) tariff plan
   * @return
   */
  private def normalizeTariff(tariff: TariffPlan): TariffPlan =
    TariffPlan(
      activationTime = tariff.activationTime,
      energyConsumptionFee = tariff.energyConsumptionFee.toCurrency,
      parkingFee = tariff.parkingFee.map(_.toCurrency),
      serviceFee = tariff.serviceFee
    )
  
  /**
   * We assume thar session settlement may take some time as in real life applications such operations usually require
   * DB querying and/or polling some other services, thus this method returns Future
   *
   * @param session - session to be billed
   * @param tariff - tariff plan with which to bill given session
   * @return - future session settlement
   */
  private def settleSessionWithTariffAsync(session: FinishedChargingSession, tariff: TariffPlan): Future[SessionSettlement] =
    Future {
      val sessionDurationHours = getSessionDurationHours(session)
      val energyTotal = (tariff.energyConsumptionFee * session.consumedEnergy).toCurrency
      val parkingTotal = (tariff.parkingFee.fold[BigDecimal](0)(_ * sessionDurationHours)).toCurrency
      val subTotal = energyTotal + parkingTotal
      val serviceTotal: BigDecimal = (subTotal * tariff.serviceFee).toCurrency
      val bottomLine: BigDecimal = subTotal + serviceTotal
      SessionSettlement(
        session = session,
        tariff = tariff,
        totals = SessionTotals(
          energyTotal = energyTotal,
          parkingTotal = parkingTotal,
          serviceTotal = serviceTotal,
          totalAll = bottomLine,
        )
      )
    }
  
  override def onMessage(msg: Command): Behavior[Command] = {
    log.debug(s"Inbound message: $msg")
    
    msg match {
      case ProcessFinishedSession(session, replayTo, requestId) =>
        findApplicableTariffPlanForSession(session) match {
          case Some(tariff) =>
            val futureSessionSettlement = settleSessionWithTariffAsync(session.copy(), tariff.copy())
            futureSessionSettlement.map(storeService.save)
  
            recipientByRequestId += requestId -> replayTo
            
            // Should be safe to use 'requestId' in this transformation function
            context.pipeToSelf(futureSessionSettlement) {
              case Success(value) => AsyncSessionSettlementResultWrap(Some(value), requestId)
              case Failure(ex) =>
                log.error(ex.getMessage)
                AsyncSessionSettlementResultWrap(None, requestId)
            }
            Behaviors.same
  
          case None =>
            replayTo ! ProcessFinishedSessionResponse(Statuses.NoApplicableTariffPlanFound, requestId)
            Behaviors.same
        }

      case AsyncSessionSettlementResultWrap(maybeSessionSettlement, requestId) =>
        recipientByRequestId.get(requestId) match {
          case Some(replayTo) => maybeSessionSettlement match {
            case Some(sessionSettlement) => replayTo ! ProcessFinishedSessionResponse(Statuses.Ok, requestId, Some(sessionSettlement))
            case None => replayTo ! ProcessFinishedSessionResponse(Statuses.UnknownError, requestId)
          }
          case None => log.error(s"No recipient found")
        }
        Behaviors.same
        
      case SubmitTariffPlan(tariffPlan, replayTo) =>
        // We need to assure the proposed tariff plan starts in the future
        if (tariffPlan.activationTime.isAfter(Instant.now().plusSeconds(config.proposedTariffAnnouncementAdvance))) {
          
          tariffPlans += normalizeTariff(tariffPlan)
          replayTo ! SubmitTariffPlanResponse(Statuses.Ok)
        } else {
          replayTo ! SubmitTariffPlanResponse(Statuses.ProposedTariffPlanStartsTooSoon)
        }
        Behaviors.same
    }
  }
}