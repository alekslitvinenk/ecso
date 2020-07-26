package com.alekslitvinenk.ecso.actors.charge

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.alekslitvinenk.ecso.TestData._
import com.alekslitvinenk.ecso.domain.ErrorCode
import com.alekslitvinenk.ecso.domain.Protocol.{FinishChargingSession, StartChargingSession}
import com.alekslitvinenk.ecso.service.{HttpClient, StubHttpClient}
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future

class ChargingTerminalActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with BeforeAndAfter {
  
  private var httpClient: HttpClient = _
  
  before {
    httpClient =
      StubHttpClient(_ => Future.successful(billSessionResponse))
  }
  
  "ChargingTerminalActor" should {
  
    "replay with ErrorCode.Ok on StartSession when all input data are correct" in {
      val chargingTerminalActor = spawn(ChargingTerminalActor(correctStationId, correctTerminalId, httpClient, ecsoConfig))
      val probe = createTestProbe[Command]()
      chargingTerminalActor ! StartSession(correctStartChargingSession, probe.ref, requestId)
  
      val response = probe.receiveMessage().asInstanceOf[StartSessionResponse]
      response.response.status.code should be(ErrorCode.Ok)
    }

    "replay with ErrorCode.TerminalAlreadyHasActiveSessionError on 2nd identical StartSession message" in {
      val probe1 = createTestProbe[Command]()
      val chargingTerminalActor = spawn(ChargingTerminalActor(correctStationId, correctTerminalId, httpClient, ecsoConfig))
  
      chargingTerminalActor ! StartSession(StartChargingSession(correctDriverId, correctStationId, correctTerminalId), probe1.ref, requestId)
  
      val probe2 = createTestProbe[Command]()
      chargingTerminalActor ! StartSession(StartChargingSession(correctDriverId, correctStationId, correctTerminalId), probe2.ref, requestId)
  
      val response = probe2.receiveMessage().asInstanceOf[StartSessionResponse]
      response.response.status.code should be(ErrorCode.TerminalAlreadyHasActiveSession)
    }

    "replay with ErrorCode.WrongTerminalError when bad terminalId supplied" in {
      val probe = createTestProbe[Command]()
      val chargingTerminalActor = spawn(ChargingTerminalActor(correctStationId, correctTerminalId, httpClient, ecsoConfig))
  
      chargingTerminalActor ! StartSession(StartChargingSession(correctDriverId, badStationId, badTerminalId), probe.ref, requestId)
  
      val response = probe.receiveMessage().asInstanceOf[StartSessionResponse]
      response.response.status.code should be(ErrorCode.WrongTerminal)
    }

    "replay with ErrorCode.IdleTerminalError when FinishSession comes before StartSession" in {
      val probe = createTestProbe[Command]()
      val chargingTerminalActor = spawn(ChargingTerminalActor(correctStationId, correctTerminalId, httpClient, ecsoConfig))
  
      chargingTerminalActor ! FinishSession(FinishChargingSession(correctDriverId, correctStationId, correctTerminalId), probe.ref, requestId)
  
      val response = probe.receiveMessage().asInstanceOf[FinishSessionResponse]
      response.response.status.code should be(ErrorCode.IdleTerminal)
    }
  
    "replay with ErrorCode.Ok when correct StartSession and FinishSession called consecutively" in {
      val chargingTerminalActor = spawn(ChargingTerminalActor(correctStationId, correctTerminalId, httpClient, ecsoConfig))
      val probe = createTestProbe[Command]()
      chargingTerminalActor ! StartSession(correctStartChargingSession, probe.ref, requestId)
    
      val probe2 = createTestProbe[Command]()
      chargingTerminalActor ! FinishSession(correctFinishChargingSession, probe2.ref, requestId)
    
      val response = probe2.receiveMessage().asInstanceOf[FinishSessionResponse]
      response.response.status.code should be(ErrorCode.Ok)
    }
  
    "replay with ErrorCode.UnknownError when Http query produces exception" in {
      httpClient = StubHttpClient(_ => Future.failed(new RuntimeException("Test exception")))
  
      val chargingTerminalActor = spawn(ChargingTerminalActor(correctStationId, correctTerminalId, httpClient, ecsoConfig))
      val probe = createTestProbe[Command]()
      chargingTerminalActor ! StartSession(correctStartChargingSession, probe.ref, requestId)
      
      val probe2 = createTestProbe[Command]()
      chargingTerminalActor ! FinishSession(correctFinishChargingSession, probe2.ref, requestId)
    
      val response = probe2.receiveMessage().asInstanceOf[FinishSessionResponse]
      response.response.status.code should be(ErrorCode.UnknownError)
    }
  }
}