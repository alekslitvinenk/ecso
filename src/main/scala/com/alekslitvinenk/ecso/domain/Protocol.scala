package com.alekslitvinenk.ecso.domain

import java.time.Instant

object Protocol {
  
  case class ActiveChargingSession(
    driverId: Long,
    startTime: Instant,
    sessionTicket: String,
  )
  
  case class FinishedChargingSession(
    driverId: Long,
    startTime: Instant,
    sessionTicket: String,
    finishTime: Instant,
    consumedEnergy: BigDecimal,
  )
  
  case class SessionTotals(
    energyTotal : BigDecimal,
    parkingTotal: BigDecimal,
    serviceTotal: BigDecimal,
    totalAll    : BigDecimal,
  )
  
  final case class SessionSettlement(
    session: FinishedChargingSession,
    tariff: TariffPlan,
    totals: SessionTotals,
  )
  
  /**
   * This implicit object is responsible for sorting (by activationTime) in ordered collections
   */
  implicit object TariffPlanOrdering extends Ordering[TariffPlan] {
    override def compare(x: TariffPlan, y: TariffPlan): Int = y.activationTime compareTo x.activationTime
  }
  
  case class TariffPlan(
    activationTime      : Instant,
    energyConsumptionFee: BigDecimal,
    parkingFee          : Option[BigDecimal],
    serviceFee          : Double,
  ) {
    require(serviceFee >= 0 && serviceFee <= 0.5, "Service fee should be within the range from 0 to 0.5")
    require(energyConsumptionFee > 0, "Energy consumption fee should be greater than 0")
  }
  
  final case class StartChargingSession(
    driverId: Long,
    stationId: Long,
    terminalId: Long,
  )
  final case class StartChargingSessionResponse(
    status: Status,
    sessionTicket: Option[String] = None,
  )
  
  final case class FinishChargingSession(
    driverId: Long,
    stationId: Long,
    terminalId: Long,
  )
  final case class FinishChargingSessionResponse(
    status: Status,
    sessionSettlement: Option[SessionSettlement] = None
  )
  
  final case class BillSession(
    finishedChargingSession: FinishedChargingSession,
    requestId: Long,
  )
  final case class BillSessionResponse(
    status: Status,
    sessionSettlement: Option[SessionSettlement] = None,
    requestId: Long
  )
  
  final case class Status(
    code: Int,
    message: String
  )
}
