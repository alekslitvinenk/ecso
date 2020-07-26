package com.alekslitvinenk.ecso

import java.util.concurrent.Executors

import akka.actor.Actor
import akka.actor.typed.{ActorRef, ActorSystem, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.{Done, actor}
import com.alekslitvinenk.ecso.actors.SettlementProcessorActor
import com.alekslitvinenk.ecso.actors.SettlementProcessorActor.Command
import com.alekslitvinenk.ecso.actors.charge.ChargingStationManagerActor
import com.alekslitvinenk.ecso.config.EcsoConfig
import com.alekslitvinenk.ecso.route.{BackofficeRoute, ChargingSessionRoute}
import com.alekslitvinenk.ecso.service.{EcsoHttpClient, HttpClient, SessionStore, SessionStoreService}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success}

object Main extends App {
  
  private val interface: String = "0.0.0.0"
  
  /**
   * To handle HTTP requests we are using Akka-Http framework as it provides fast and convenient way to build
   * non-blocking Http servers
   */
  val system: ActorSystem[Done] = ActorSystem[Done](Behaviors.setup[Done] { context =>
    implicit val classicSystem: actor.ActorSystem = context.system.toClassic
    implicit val materializer: Materializer = Materializer(context.system.toClassic)
    implicit val ec: ExecutionContextExecutor = context.system.executionContext
    
    val ecsoConfig = EcsoConfig(ConfigFactory.load())
  
    /**
     * For the sake of simplicity we're going to use fixed thread pool
     * with just a 3 threads configured. It should be totally enough for our needs.
     * Besides, fixed pool gives us better memory footprint
     */
    val httpConnectionEc: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(3))
    
    val httpClient: HttpClient = EcsoHttpClient(ecsoConfig)(classicSystem, httpConnectionEc)
    
    val sessionStoreService: SessionStore = SessionStoreService()
    val settlementProcessor: ActorRef[Command] = context.spawn(
      SettlementProcessorActor(sessionStoreService, ecsoConfig),
      "SettlementProcessor-actor",
    )
    
    val appRoute: Route =
      ChargingSessionRoute(
        system,
        context.spawn(ChargingStationManagerActor(httpClient, ecsoConfig), "ChargingStationManager-actor"),
      ).route ~
      BackofficeRoute(
        system,
        ecsoConfig,
        settlementProcessor,
        sessionStoreService,
      ).route
    
    val serverBinding: Future[Http.ServerBinding] = Http()(classicSystem).bindAndHandle(appRoute, interface, 8080)
    serverBinding.onComplete {
      case Success(bound) =>
        println(s"Server online at http://$interface:${bound.localAddress.getPort}/")
      case Failure(e) =>
        Console.err.println(s"Server could not start!")
        e.printStackTrace()
        context.self ! Done
    }
    Behaviors.receiveMessage {
      case Done =>
        Behaviors.stopped
    }
    
  }, "ECSO-Actor")
}
