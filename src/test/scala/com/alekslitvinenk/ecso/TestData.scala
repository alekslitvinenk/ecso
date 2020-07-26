package com.alekslitvinenk.ecso

import java.time.Instant

import com.alekslitvinenk.ecso.config.{DefaultTariffPlan, EcsoConfig, SupervisorCredentials}
import com.alekslitvinenk.ecso.domain.Protocol.{BillSessionResponse, FinishChargingSession, FinishedChargingSession, SessionSettlement, SessionTotals, StartChargingSession, TariffPlan}
import com.alekslitvinenk.ecso.domain.Statuses

object TestData {
  val correctDriverId = 1L
  val correctStationId = 1L
  val correctTerminalId = 1L
  val badStationId = 2L
  val badTerminalId = 2L
  val requestId = 1L
  
  val timeRef: Instant = Instant.now()
  val sessionDurationSec = 360
  val sessionStartTime: Instant = timeRef.minusSeconds(sessionDurationSec)
  val sessionFinishTime: Instant = timeRef
  val energyConsumed = 255
  val sessionTicket = ""
  
  val tariffActivationTime: Instant = timeRef
  val energyPrice = 1
  val parkingPriceOpt: Option[BigDecimal] = Some(2)
  val serviceFee = 0.1
  
  val correctStartChargingSession: StartChargingSession = StartChargingSession(
    correctDriverId,
    correctStationId,
    correctTerminalId
  )
  
  val correctFinishChargingSession: FinishChargingSession = FinishChargingSession(
    correctDriverId,
    correctStationId,
    correctTerminalId
  )
  
  val finishedSession: FinishedChargingSession = FinishedChargingSession(
    driverId = correctDriverId,
    startTime = sessionStartTime,
    sessionTicket = sessionTicket,
    finishTime = sessionFinishTime,
    consumedEnergy = energyConsumed,
  )
  
  val tariffPlan: TariffPlan = TariffPlan(
    activationTime = tariffActivationTime,
    energyConsumptionFee = energyPrice,
    parkingFee = parkingPriceOpt,
    serviceFee = serviceFee
  )
  
  val sessionTotals: SessionTotals = SessionTotals(
    energyTotal = BigDecimal("255.00"),
    parkingTotal = BigDecimal("2.00"),
    serviceTotal = BigDecimal("25.70"),
    totalAll = BigDecimal("282.70")
  )
  
  val sessionSettlement: SessionSettlement = SessionSettlement(
    session = finishedSession,
    tariff = tariffPlan,
    totals = sessionTotals
  )
  
  val billSessionResponse: BillSessionResponse = BillSessionResponse(
    status = Statuses.Ok,
    sessionSettlement = Some(
      value = sessionSettlement
    ),
    requestId = 1L,
  )
  
  val supervisorUsername = "supervisor"
  val supervisorPassword = "password"
  val supervisorCredentials: SupervisorCredentials = SupervisorCredentials(
    userName = supervisorUsername,
    password = supervisorPassword,
  )
  
  val proposedTariffAnnouncementAdvance = 360
  
  val defaultTariffPlan: DefaultTariffPlan = DefaultTariffPlan(
    energyConsumptionFee = tariffPlan.energyConsumptionFee,
    parkingFee = tariffPlan.parkingFee,
    serviceFee = tariffPlan.serviceFee,
  )
  
  val billSessionUrl = "billSessionUrl"
  
  val defaultEnergyConsumptionRate = 1
  
  val ecsoConfig: EcsoConfig = EcsoConfig(
    backofficeUrl = billSessionUrl,
    defaultEnergyConsumption = defaultEnergyConsumptionRate,
    supervisorCredentials = supervisorCredentials,
    useDefaultTariffPlan = true,
    proposedTariffAnnouncementAdvance = proposedTariffAnnouncementAdvance,
    defaultTariffPlan = defaultTariffPlan,
  )
}
