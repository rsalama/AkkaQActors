package org.websocket

import akka.actor._
import akka.routing.{ RoundRobinRouter, Broadcast }
import akka.util.duration._
import akka.util.Duration
import Messages._
import org.mashupbots.socko.handlers.WebSocketBroadcastText

class Master(numWorkers: Int,
  numMessages: Int,
  numElements: Int,
  listener: ActorRef) extends Actor {

  var pi: Double = 0.0
  var numResults: Int = 0
  val start: Long = System.currentTimeMillis

  val workerRouter =
    context.actorOf(Props[Worker].withRouter(RoundRobinRouter(numWorkers)), name = "workerRouter")

  val broadcaster = context.actorFor("/user/webSocketBroadcaster")
  broadcaster ! WebSocketBroadcastText("workerRouter with %d workers created".format(numWorkers))

  def receive = {
    case Calculate =>
      for (i <- 0 until numMessages)
        workerRouter ! Work(i * numElements, numElements)
    case Result(value) =>
      pi += value
      numResults += 1
      //      println("Result: %s, pi: %.6g".format(value, pi))
      if (numResults == numMessages) {
        listener ! PiApproximation(pi, duration = (System.currentTimeMillis - start).millis)
        workerRouter ! Broadcast(Shutdown)
        context.stop(self)
      }
  }
}
