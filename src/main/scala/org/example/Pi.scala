package org.example

import akka.actor._
import akka.routing.RoundRobinRouter
import akka.util.Duration
import akka.util.duration._

object Pi extends App {

  calculate(nrOfWorkers = 4, nrOfElements = 10000, nrOfMessages = 10000)

  sealed trait PiMessage
  case object Calculate extends PiMessage
  case class Work(start: Int, nrOfElements: Int) extends PiMessage
  case class Result(value: Double) extends PiMessage
  case class PiApproximation(pi: Double, duration: Duration)

  sealed trait Trampoline[+A] {
    def run: A = this match {
      case Done(a) => a
      case More(t) => t().run
    }
  }
  case class Done[A](a: A) extends Trampoline[A]
  case class More[A](a: () => Trampoline[A]) extends Trampoline[A]

  class Worker extends Actor {

    def calculatePi(start: Int, nrOfElements: Int): Double = {
      (0.0 /: (start to (start + nrOfElements)))((acc, i) => acc + f(i))
    }

    def f(i: Int) = 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
    def g(start: Int, nrOfElements: Int): Double = {
      (0.0 /: (start to (start + nrOfElements))) { (acc, i) =>
        {
          println("acc: %g, f[%d]: %.6g. term: %.6g".format(acc, i, f(i), acc + f(i)))
          acc + f(i)
        }
      }
    }

    def trampPi(start: Int, nelem: Int): Double = {
      def term(acc: Double, i: Int): Trampoline[Double] = {
        println("acc: %g i: %d, nelem: %d".format(acc, i, nelem))
        if (i == nelem) Done(acc) else More(() => term(acc + f(i), i + 1))
      }
      term(0.0, 0).run
    }

    def receive = {
      case Work(start, nrOfElements) =>
        sender ! Result(calculatePi(start, nrOfElements)) // perform the work
    }
  }

  class Master(nrOfWorkers: Int,
    nrOfMessages: Int,
    nrOfElements: Int,
    listener: ActorRef) extends Actor {

    var pi: Double = _
    var nrOfResults: Int = _
    val start: Long = System.currentTimeMillis

    val workerRouter = context.actorOf(
      Props[Worker].withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")

    def receive = {
      case Calculate =>
        for (i <- 0 until nrOfMessages)
          workerRouter ! Work(i * nrOfElements, nrOfElements)
      case Result(value) =>
        pi += value
        nrOfResults += 1
        if (nrOfResults == nrOfMessages) {
          listener ! PiApproximation(pi, duration = (System.currentTimeMillis - start).millis)
          context.stop(self)
        }
    }
  }

  class Listener extends Actor {
    def receive = {
      case PiApproximation(pi, duration) =>
        println("\n\tPi approximation: \t\t%s\n\tCalculation time: \t%s".format(pi, duration))
        context.system.shutdown()
    }
  }

  def calculate(nrOfWorkers: Int, nrOfElements: Int, nrOfMessages: Int) {
    // Create an Akka system
    val system = ActorSystem("PiSystem")

    // create the result listener, which will print the result and 
    // shutdown the system
    val listener = system.actorOf(Props[Listener], name = "listener")

    // create the master
    val master = system.actorOf(Props(new Master(
      nrOfWorkers, nrOfMessages, nrOfElements, listener)),
      name = "master")

    // start the calculation
    master ! Calculate

  }
}