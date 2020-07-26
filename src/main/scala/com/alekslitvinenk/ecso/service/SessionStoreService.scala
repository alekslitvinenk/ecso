package com.alekslitvinenk.ecso.service
import com.alekslitvinenk.ecso.domain.Protocol.SessionSettlement

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}

object SessionStoreService {
  def apply()(implicit ecBlocking: ExecutionContext): SessionStoreService = new SessionStoreService()
}

class SessionStoreService()(implicit ecBlocking: ExecutionContext) extends SessionStore {
  /**
   * TrieMap supports atomic inserts, updates and removals of elements
   * as well as retrieving information about the structure as a whole e.g size
   * [[http://lampwww.epfl.ch/~prokopec/ctries-snapshot.pdf]]
   */
  private val store = TrieMap.empty[String, SessionSettlement]
  
  override def save(e: SessionSettlement): Future[Unit] = Future { store.put(e.session.sessionTicket, e) }
  
  override def getAll: Future[Iterable[SessionSettlement]] = Future { store.values }
  
  override def getAllByDriverId(driverId: Long): Future[Iterable[SessionSettlement]] =
    Future { store.values.filter(_.session.driverId == driverId) }
}
