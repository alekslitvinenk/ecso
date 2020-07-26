package com.alekslitvinenk.ecso.util

import scala.math.BigDecimal.RoundingMode

object CurrencyFormatUtils {
  
  implicit class BigDecimalToCurrencyConverter(value: BigDecimal) {
  
    /**
     * Rounds given amount to the nearest 2 digits decimal value using HALF-UP rounding mode
     * @return
     */
    def toCurrency: BigDecimal = value.setScale(2, RoundingMode.HALF_UP)
  }
}
