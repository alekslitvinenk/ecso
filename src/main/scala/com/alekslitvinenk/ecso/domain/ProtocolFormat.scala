package com.alekslitvinenk.ecso.domain

import java.time.Instant

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.alekslitvinenk.ecso.actors.SettlementProcessorActor.SubmitTariffPlanResponse
import com.alekslitvinenk.ecso.domain.Protocol.{BillSession, BillSessionResponse, FinishChargingSession, FinishChargingSessionResponse, FinishedChargingSession, SessionSettlement, SessionTotals, StartChargingSession, StartChargingSessionResponse, Status, TariffPlan}
import com.alekslitvinenk.ecso.util.InstantFormatUtils._
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

object ProtocolFormat {
  object JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val statusCodec: RootJsonFormat[Status] = jsonFormat2(Status)
    implicit val startChargingSessionCodec: RootJsonFormat[StartChargingSession] = jsonFormat3(StartChargingSession)
    implicit val startChargingSessionResponseCodec: RootJsonFormat[StartChargingSessionResponse] = jsonFormat2(StartChargingSessionResponse)
    implicit val finishChargingSessionCodec: RootJsonFormat[FinishChargingSession] = jsonFormat3(FinishChargingSession)
    
    implicit object InstantFormat extends RootJsonFormat[Instant] {
      override def write(obj: Instant): JsValue = JsString(obj.toCanonical)
      override def read(json: JsValue): Instant = Instant.parse(json.asInstanceOf[JsString].value)
    }
    
    implicit val sessionTotalsCodec: RootJsonFormat[SessionTotals] = jsonFormat4(SessionTotals)
    implicit val tariffPlanCodec: RootJsonFormat[TariffPlan] = jsonFormat4(TariffPlan)
    implicit val finishedChargingSessionCodec: RootJsonFormat[FinishedChargingSession] = jsonFormat5(FinishedChargingSession)
    implicit val sessionSettlementCodec: RootJsonFormat[SessionSettlement] = jsonFormat3(SessionSettlement)
    implicit val finishChargingSessionResponseCodec: RootJsonFormat[FinishChargingSessionResponse] = jsonFormat2(FinishChargingSessionResponse)
    implicit val submitTariffPlanResponseCodec: RootJsonFormat[SubmitTariffPlanResponse] = jsonFormat1(SubmitTariffPlanResponse)
    implicit val processBillSessionCodec: RootJsonFormat[BillSession] = jsonFormat2(BillSession)
    implicit val processBillSessionResponseCodec: RootJsonFormat[BillSessionResponse] = jsonFormat3(BillSessionResponse)
  }
}
