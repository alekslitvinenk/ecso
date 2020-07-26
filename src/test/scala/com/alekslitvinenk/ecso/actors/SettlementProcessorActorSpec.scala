package com.alekslitvinenk.ecso.actors

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.alekslitvinenk.ecso.TestData._
import com.alekslitvinenk.ecso.actors.SettlementProcessorActor.{Command, ProcessFinishedSession, ProcessFinishedSessionResponse, SubmitTariffPlan, SubmitTariffPlanResponse}
import com.alekslitvinenk.ecso.domain.ErrorCode
import com.alekslitvinenk.ecso.service.{SessionStore, SessionStoreService}
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext.Implicits._

class SettlementProcessorActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with BeforeAndAfter {
  
  private var sessionStore: SessionStore = _
  
  before {
    sessionStore = SessionStoreService()
  }
  
  "SettlementProcessorActor" should {
    
    "reply with ErrorCode.Ok on correct ProcessSession" in {
      val sessionSettlementActor = spawn(SettlementProcessorActor(sessionStore, ecsoConfig))
      val probe = createTestProbe[Command]()
      sessionSettlementActor ! ProcessFinishedSession(finishedSession, probe.ref, 1L)
  
      val response = probe.receiveMessage().asInstanceOf[ProcessFinishedSessionResponse]
      response.status.code should be(ErrorCode.Ok)
      response.settlement.get.totals should be(sessionTotals)
    }
  
    "reply with ErrorCode.NoApplicableTariffPlanFound on correct ProcessSession and when no default tariff specified" in {
      val sessionSettlementActor = spawn(SettlementProcessorActor(sessionStore, ecsoConfig.copy(useDefaultTariffPlan = false)))
      val probe = createTestProbe[Command]()
      sessionSettlementActor ! ProcessFinishedSession(finishedSession, probe.ref, 1L)
    
      val response = probe.receiveMessage().asInstanceOf[ProcessFinishedSessionResponse]
      response.status.code should be(ErrorCode.NoApplicableTariffPlanFound)
    }
    
    "replay with ErrorCode.Ok on SubmitTariffPlan when tariff starts in 1h" in {
      val sessionSettlementActor = spawn(SettlementProcessorActor(sessionStore, ecsoConfig))
      val probe = createTestProbe[Command]()
      sessionSettlementActor ! SubmitTariffPlan(tariffPlan.copy(activationTime = Instant.now().plusSeconds(3600)), probe.ref)
  
      val response = probe.receiveMessage().asInstanceOf[SubmitTariffPlanResponse]
      response.status.code should be(ErrorCode.Ok)
    }
  
    "replay with ErrorCode.ProposedTariffPlanStartsTooSoon on SubmitTariffPlan when tariff starts now" in {
      val sessionSettlementActor = spawn(SettlementProcessorActor(sessionStore, ecsoConfig))
      val probe = createTestProbe[Command]()
      sessionSettlementActor ! SubmitTariffPlan(tariffPlan, probe.ref)
    
      val response = probe.receiveMessage().asInstanceOf[SubmitTariffPlanResponse]
      response.status.code should be(ErrorCode.ProposedTariffPlanStartsTooSoon)
    }
  }
}
