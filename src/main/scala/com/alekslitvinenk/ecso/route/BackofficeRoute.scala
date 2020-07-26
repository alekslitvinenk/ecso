package com.alekslitvinenk.ecso.route

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import akka.util.Timeout
import com.alekslitvinenk.ecso.actors.SettlementProcessorActor._
import com.alekslitvinenk.ecso.config.{EcsoConfig, SupervisorCredentials}
import com.alekslitvinenk.ecso.domain.Protocol.{BillSession, BillSessionResponse, TariffPlan}
import com.alekslitvinenk.ecso.service.SessionStore
import com.alekslitvinenk.ecso.util.SessionSettlementUtils._

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * BackofficeRoute represent endponts exposed by BO
 * It includes route for submitting finished charging session for further processing and billing,
 * endpoint for supervisor to submit new tariff plans,
 * as well as endpoint for bookkeeper for he/she might want to examine sessions and,
 * least but not last, the endpoint for driver to overview his/her own charging sessions
 *
 * Usually access to any BO edpoint entails full-blown authentication/authorization,
 * but in this route, for the sake of simplicity,  we implemented it only for supervisor
 */
object BackofficeRoute {
  def apply(system: ActorSystem[_],
            ecsoConfig: EcsoConfig,
            settlementProcessor: ActorRef[Command],
            sessionStore: SessionStore): BackofficeRoute =
    new BackofficeRoute(system, ecsoConfig, settlementProcessor, sessionStore)
}

class BackofficeRoute(system: ActorSystem[_],
                      ecsoConfig: EcsoConfig,
                      settlementProcessor: ActorRef[Command],
                      sessionStore: SessionStore) extends HasRoute {
  
  import com.alekslitvinenk.ecso.domain.ProtocolFormat.JsonSupport._
  
  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler: Scheduler = system.scheduler
  
  private val supervisorCredentials = ecsoConfig.supervisorCredentials
  
  private def ecsoBasicAuthenticator(credentials: Credentials): Option[String] = credentials match {
      case p @ Credentials.Provided(id) =>
        getPasswordByUserId(id).flatMap { password =>
          if (p.verify(password)) Some(id)
          else None
        }
      case _ => None
    }
  
  private def getPasswordByUserId(id: String): Option[String] = {
    if (id == supervisorCredentials.userName) Some(supervisorCredentials.password)
    else None
  }
  
  lazy val route: Route = pathPrefix("backoffice") {
    path("bill-session") {
      post {
        entity(as[BillSession]) { billSession =>
          val futureRes: Future[Command] = settlementProcessor.ask{ ref =>
            ProcessFinishedSession(
              billSession.finishedChargingSession,
              ref,
              billSession.requestId,
            )
          }
          
          onSuccess(futureRes) {
            case ProcessFinishedSessionResponse(status, requestId, settlement) => complete(BillSessionResponse(
              status,
              settlement,
              requestId,
            ))

            case _ => complete(StatusCodes.InternalServerError)
          }
        }
      }
    } ~ path("bookkeeper") {
      get {
        onSuccess(sessionStore.getAll) { res =>
          complete(HttpEntity(ContentTypes.`text/csv(UTF-8)`, res.makeCSV))
        }
      }
    } ~ path("driver") {
      get {
        parameter('driverId.as[Long]) { driverId =>
          onSuccess(sessionStore.getAllByDriverId(driverId)) { res =>
            complete(HttpEntity(ContentTypes.`text/csv(UTF-8)`, res.makeCSV))
          }
        }
      }
    } ~ Route.seal {
      pathPrefix("supervisor") {
        path("tariff") {
          authenticateBasic("Supervisor domain", ecsoBasicAuthenticator) { _ =>
            post {
              entity(as[TariffPlan]) { tariffPlan =>
                val futureRes: Future[Command] = settlementProcessor.ask(ref => SubmitTariffPlan(
                  tariffPlan, ref)
                )
            
                onSuccess(futureRes) {
                  case res @ SubmitTariffPlanResponse(_) => complete(res)
                  case _ => complete(StatusCodes.InternalServerError)
                }
              }
            }
          }
        }
      }
    }
  }
}
