package com.alekslitvinenk.ecso.service

import com.alekslitvinenk.ecso.domain.Protocol.SessionSettlement

import scala.concurrent.Future

/**
 * SessionStore provides API for interaction with database.
 * Usually this operation takes some time (especially when DB is on another host), so here we'll use Future
 * to interact with our improvised DB in a async way
 */
trait SessionStore {
  def save(e: SessionSettlement): Future[Unit]
  def getAll: Future[Iterable[SessionSettlement]]
  def getAllByDriverId(driverId: Long): Future[Iterable[SessionSettlement]]
}
