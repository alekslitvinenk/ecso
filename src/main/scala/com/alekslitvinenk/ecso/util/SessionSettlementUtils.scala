package com.alekslitvinenk.ecso.util

import java.time.Instant

import com.alekslitvinenk.ecso.domain.Protocol.SessionSettlement
import com.alekslitvinenk.ecso.util.InstantFormatUtils._

object SessionSettlementUtils {
  
  implicit class SessionSettlementOps(sessionSettlement: SessionSettlement) {
  
    /**
     * Converts given sessionSettlement into CSV line according to RFC 4180 [[https://tools.ietf.org/html/rfc4180]]
     * @return
     */
    def toCSV: String =
      sessionSettlement.productIterator.toList.map(p => reduce(p.asInstanceOf[Product])).reduce(_ + "," + _) + "\n"
    
    private def reduce(product: Product) = product.productIterator.reduce(combineComponents).asInstanceOf[String]
    
    private def combineComponents(a: Any, b: Any) = {
      val aStr = a.toString
      val bStr = b match {
        case instant: Instant => instant.toCanonical
        case _ => b.toString
      }
      
      aStr + "," + bStr
    }
  }
  
  implicit class SessionCollectionOps(sessions: Iterable[SessionSettlement]) {
  
    /**
     * Creates a CSV file from a given collection of settled sessions
     * @return
     */
    def makeCSV: String = sessions.aggregate("")((p, session) => p + session.toCSV, _ + _)
  }
}
