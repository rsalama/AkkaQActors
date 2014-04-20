package org.websocket

object Utils {
  implicit def toDouble(o: Object) = o.asInstanceOf[Double]

  def fatal(msg: String): Nothing = {
    System.err.println(msg)
    exit(1)
  }
}