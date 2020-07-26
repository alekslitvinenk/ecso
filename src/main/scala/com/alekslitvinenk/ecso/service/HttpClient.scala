package com.alekslitvinenk.ecso.service

import com.alekslitvinenk.ecso.domain.Protocol.{BillSessionResponse, FinishedChargingSession}

import scala.concurrent.Future

trait HttpClient {
  
  def requestSessionSettlement(finishedChargingSession: FinishedChargingSession,
                               requestId: Long): Future[BillSessionResponse]
}
