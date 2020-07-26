package com.alekslitvinenk.ecso.domain

import com.alekslitvinenk.ecso.domain.Protocol.Status

object ErrorCode {
  val Ok = 0
  val WrongTerminal = 1
  val IdleTerminal = 2
  val TerminalAlreadyHasActiveSession = 3
  val WrongDriverId = 4
  val WrongStation = 5
  val NoApplicableTariffPlanFound = 6
  val CannotFindRecipientByRequestId = 7
  val ProposedTariffPlanStartsTooSoon = 8
  
  val UnknownError = 1000
}

object Statuses {
  val Ok: Status = Status(ErrorCode.Ok, "Ok")
  val WrongTerminal: Status = Status(ErrorCode.WrongTerminal, "Wrong terminal")
  val IdleTerminal: Status = Status(ErrorCode.IdleTerminal, "Idle terminal")
  val TerminalAlreadyHasActiveSession: Status =
    Status(ErrorCode.TerminalAlreadyHasActiveSession, "Terminal already has active session")
  val WrongDriverId: Status = Status(ErrorCode.WrongDriverId, "Wrong driverId")
  val WrongStation: Status = Status(ErrorCode.WrongStation, "Wrong station")
  val NoApplicableTariffPlanFound: Status = Status(ErrorCode.NoApplicableTariffPlanFound, "No applicable tariff plan found")
  val CannotFindRecipientByRequestId: Status = Status(ErrorCode.CannotFindRecipientByRequestId, "Cannot find recipient by requestId")
  val ProposedTariffPlanStartsTooSoon: Status =
    Status(ErrorCode.ProposedTariffPlanStartsTooSoon, "Proposed tariff plan starts too soon")
  val UnknownError: Status = Status(ErrorCode.UnknownError, "Unknown error")
}
