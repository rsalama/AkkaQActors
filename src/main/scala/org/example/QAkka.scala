package org.example

import java.io.IOException
import java.net.ServerSocket
import scala.sys.process._
import akka.actor._
import akka.routing.{ RoundRobinRouter, Broadcast }
import akka.util.duration._
import akka.util.Duration
import kx._
import java.net.InetAddress

object QAkka extends App {
  implicit def toDouble(o: Object) = o.asInstanceOf[Double]

  sealed trait Message
  case object Calculate extends Message
  case class Work(start: Int, numElements: Int) extends Message
  case object Wait extends Message
  case class Result(value: Double) extends Message
  case object Shutdown extends Message
  case class PiApproximation(pi: Double, duration: Duration)

  def fatal(msg: String): Nothing = {
    System.err.println(msg)
    exit(1)
  }

  class Worker extends Actor {
    val port: Int = try {
      val s = new ServerSocket(0) // a port of 0 creates a socket on any free port.
      val p = s.getLocalPort()
      s.close()
      p
    } catch {
      case e: IOException => fatal("Could not listen on any port; exiting.")
    }

    // create the Q process
    def echo(src:String)(msg:String) = println("%s: %s".format(src, msg)) 
    val plog = ProcessLogger(echo("STDOUT"), echo("STDERR"))

    val q = "q -p %d".format(port).run(/*plog*/)
    Thread.sleep(1000) // TODO -- q should tell the worker it is ready

    val k = new K(InetAddress.getLocalHost().getHostName(), port)
    k.ks("f:{[i] 4.0 * (1 - (i mod 2) * 2) % ((2 * i) + 1)}")
    k.ks("calcPi:{[start;nelem] {[acc;i] acc+f[i]}/[f[start],1+start+til nelem]}")
    k.ks(".z.ps:{[x] h:.z.w; (neg h) value x;}")

    def receive = {
      case Work(start, numElements) =>
        k.ks("calcPi", start, numElements)
        self ! Wait
      case Wait =>
        val p:Double = k.k()
        sender ! Result(p) 
      case Shutdown =>
        context.stop(self)
        k.ks("exit 0") // end q
        q.exitValue
    }
  }

  class Master(numWorkers: Int,
    numMessages: Int,
    numElements: Int,
    listener: ActorRef) extends Actor {

    var pi: Double = 0.0
    var numResults: Int = 0
    val start: Long = System.currentTimeMillis

    val workerRouter =
      context.actorOf(Props[Worker].withRouter(RoundRobinRouter(numWorkers)), name = "workerRouter")

    def receive = {
      case Calculate =>
        for (i <- 0 until numMessages)
          workerRouter ! Work(i * numElements, numElements)
      case Result(value) =>
        pi += value
        numResults += 1
//        println("Result: %s, pi: %.6g".format(value, pi))
        if (numResults == numMessages) {
          listener ! PiApproximation(pi, duration = (System.currentTimeMillis - start).millis)
          workerRouter ! Broadcast(Shutdown)
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

  // Create an Akka system
  val system = ActorSystem("PiSystem")
  val (numWorkers, numElements, numMessages) = (10, 1000, 1000)

  // create the result listener, which will print the result and shutdown the system
  val listener = system.actorOf(Props[Listener], name = "listener")

  // create the master
  val master = system.actorOf(Props(new Master(numWorkers, numMessages, numElements, listener)), name = "master")

  // start the calculation
  master ! Calculate
}