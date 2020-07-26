package com.alekslitvinenk.ecso.actors

import akka.actor.typed.ActorRef
import com.alekslitvinenk.ecso.domain.Protocol

package object charge {
  trait Command
  
  final case class StartSession(
    startSession: Protocol.StartChargingSession,
    replyTo: ActorRef[Command],
    requestId: Long,
  ) extends Command
  
  final case class StartSessionResponse(
    response: Protocol.StartChargingSessionResponse,
    requestId: Long,
  ) extends Command
  
  final case class FinishSession(
    finishSession: Protocol.FinishChargingSession,
    replyTo: ActorRef[Command],
    requestId: Long,
  ) extends Command
  
  final case class FinishSessionResponse(
    response: Protocol.FinishChargingSessionResponse,
    requestId: Long,
  ) extends Command
}
