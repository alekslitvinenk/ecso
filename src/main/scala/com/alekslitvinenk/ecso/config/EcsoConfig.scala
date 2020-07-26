package com.alekslitvinenk.ecso.config

import com.typesafe.config.Config
import pureconfig.ConfigSource

import pureconfig.generic.auto._

object EcsoConfig {
  def apply(config: Config): EcsoConfig =
    ConfigSource.fromConfig(config).at("ecso").loadOrThrow[EcsoConfig]
}

/**
 *
 * @param backofficeUrl - The URL endpoint which is called by ChargingStationTerminal to bill the finished charging session
 * @param defaultEnergyConsumption - default vehicle's energy consumption (kWh)
 * @param supervisorCredentials - supervisor credentials
 */
case class EcsoConfig(
  backofficeUrl: String,
  defaultEnergyConsumption: BigDecimal,
  supervisorCredentials: SupervisorCredentials,
  useDefaultTariffPlan: Boolean,
  proposedTariffAnnouncementAdvance: Int,
  defaultTariffPlan: DefaultTariffPlan,
) {
  require(defaultEnergyConsumption > 0, "Default energy consumption rate should be greater than 0")
  require(proposedTariffAnnouncementAdvance > 0, "Announcement period should be greater than 0")
}
