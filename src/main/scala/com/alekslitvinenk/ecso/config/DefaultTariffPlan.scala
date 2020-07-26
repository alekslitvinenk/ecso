package com.alekslitvinenk.ecso.config

import com.typesafe.config.Config
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object DefaultTariffPlan {
  def apply(config: Config): DefaultTariffPlan =
    ConfigSource.fromConfig(config).at("default-tariff-plan").loadOrThrow[DefaultTariffPlan]
}

case class DefaultTariffPlan(
  energyConsumptionFee: BigDecimal,
  parkingFee          : Option[BigDecimal],
  serviceFee          : Double,
)
