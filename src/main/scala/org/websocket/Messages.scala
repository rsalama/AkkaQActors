package org.websocket

import akka.util.Duration

object Messages {
  sealed trait Message
  case object Calculate extends Message
  case class Work(start: Int, numElements: Int) extends Message
  case object Wait extends Message
  case class Result(value: Double) extends Message
  case object Shutdown extends Message
  case class PiApproximation(pi: Double, duration: Duration)
}