package com.alekslitvinenk.ecso.service

import com.alekslitvinenk.ecso.domain.Protocol.{BillSessionResponse, FinishedChargingSession}

import scala.concurrent.Future

object StubHttpClient {
  def apply(reqToResp: FinishedChargingSession => Future[BillSessionResponse]): StubHttpClient = new StubHttpClient(reqToResp)
}

class StubHttpClient(reqToResp: FinishedChargingSession => Future[BillSessionResponse]) extends HttpClient {
  
  override def requestSessionSettlement(finishedChargingSession: FinishedChargingSession,
                                        requestId: Long): Future[BillSessionResponse] = reqToResp(finishedChargingSession)
}
