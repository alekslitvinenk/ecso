package com.alekslitvinenk.ecso.service

import akka.actor
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.util.ByteString
import com.alekslitvinenk.ecso.config.EcsoConfig
import com.alekslitvinenk.ecso.domain.Protocol.{BillSession, BillSessionResponse, FinishedChargingSession}
import com.alekslitvinenk.ecso.domain.ProtocolFormat.JsonSupport.{processBillSessionCodec, processBillSessionResponseCodec}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Http client for interacting with web-based services
 */
object EcsoHttpClient {
  
  def apply(config: EcsoConfig)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext): EcsoHttpClient =
    new EcsoHttpClient(config)(actorSystem, executionContext)
}

class EcsoHttpClient(config: EcsoConfig)(implicit actorSystem: actor.ActorSystem, executionContext: ExecutionContext) extends HttpClient {
  
  def requestSessionSettlement(finishedChargingSession: FinishedChargingSession,
                               requestId: Long): Future[BillSessionResponse] = {
    
    val futureRes: Future[BillSessionResponse] = Http().singleRequest(
      HttpRequest(
        method = HttpMethods.POST,
        uri = config.backofficeUrl,
        entity = HttpEntity(
          ContentTypes.`application/json`,
          processBillSessionCodec.write(BillSession(finishedChargingSession, requestId)).toString()),
      )
    ).flatMap { httpResponse =>
      val entity = httpResponse.entity
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body =>
        val bodyStr = body.utf8String
        actorSystem.log.debug(s"unmarshalledMsg: $bodyStr")
        processBillSessionResponseCodec.read(body.utf8String.parseJson)
      }
    }
    
    futureRes
  }
}
