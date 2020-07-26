package com.alekslitvinenk.ecso.route

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.alekslitvinenk.ecso.actors.charge._
import com.alekslitvinenk.ecso.domain.Protocol.{FinishChargingSession, StartChargingSession}
import com.alekslitvinenk.ecso.util.RequestUtils

import scala.concurrent.Future
import scala.concurrent.duration._

object ChargingSessionRoute {
  def apply(system: ActorSystem[_], chargingStationManager: ActorRef[Command]): ChargingSessionRoute =
    new ChargingSessionRoute(system, chargingStationManager)
}

class ChargingSessionRoute(system: ActorSystem[_], chargingStationManagerActor: ActorRef[Command]) extends HasRoute {
  
  import com.alekslitvinenk.ecso.domain.ProtocolFormat.JsonSupport._
  
  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler: Scheduler = system.scheduler
  
  lazy val route: Route = pathPrefix("charging" ) {
    path("start") {
      post {
        entity(as[StartChargingSession]) { startSession =>
          println(startSession)
          val futureRes: Future[Command] = chargingStationManagerActor.ask(ref => StartSession(
            startSession,
            ref,
            RequestUtils.generateRequestId
          ))
          
          onSuccess(futureRes) {
            case StartSessionResponse(response, _) => complete(response)
            case _ => complete(StatusCodes.InternalServerError)
          }
        }
      }
    } ~ path("finish") {
      post {
        entity(as[FinishChargingSession]) { finishSession =>
          println(finishSession)
          val futureRes: Future[Command] = chargingStationManagerActor.ask(ref => FinishSession(
            finishSession,
            ref,
            RequestUtils.generateRequestId)
          )

          onSuccess(futureRes) {
            case FinishSessionResponse(response, _) => complete(response)
            case _ => complete(StatusCodes.InternalServerError)
          }
        }
      }
    }
  }
}
