package com.alekslitvinenk.ecso.config

import com.typesafe.config.Config
import pureconfig.ConfigSource

import pureconfig.generic.auto._

object SupervisorCredentials {
  def apply(config: Config): SupervisorCredentials =
    ConfigSource.fromConfig(config).at("supervisor-credentials").loadOrThrow[SupervisorCredentials]
}

case class SupervisorCredentials(userName: String, password: String)
