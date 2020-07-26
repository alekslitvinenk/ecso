package com.alekslitvinenk.ecso.route

import akka.http.scaladsl.server.Route

trait HasRoute {
  def route: Route
}
