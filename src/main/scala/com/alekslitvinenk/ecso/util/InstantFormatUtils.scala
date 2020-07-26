package com.alekslitvinenk.ecso.util

import java.time.Instant

object InstantFormatUtils {
  
  implicit class InstantConverters(inst: Instant) {
    /**
     * Converts given instant of time into canonical RFC 3339 [[https://tools.ietf.org/html/rfc3339]] representation
     * @return string representation of an instant
     */
    def toCanonical: String = Instant.ofEpochSecond(inst.toEpochMilli / 1000).toString
  }
}
