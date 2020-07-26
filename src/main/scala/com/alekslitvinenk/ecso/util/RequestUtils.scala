package com.alekslitvinenk.ecso.util

import java.util.concurrent.atomic.AtomicLong

object RequestUtils {
  
  private val requestIdSeqGenerator: AtomicLong = new AtomicLong(1)
  
  def generateRequestId: Long = requestIdSeqGenerator.getAndIncrement()
}
