package org.websocket

import scala.sys.process._
import akka.actor._
import java.net.{ ServerSocket, InetAddress }
import java.io.IOException
import kx.K
import Messages._
import Utils._
import org.mashupbots.socko.handlers.WebSocketBroadcastText

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
  def echo(src: String)(msg: String) = println("%s: %s".format(src, msg))
  val plog = ProcessLogger(echo("STDOUT"), echo("STDERR"))

  val q = "q -p %d".format(port).run( /*plog*/ )
  Thread.sleep(1000) // TODO -- q should tell the worker it is ready

  val k = new K(InetAddress.getLocalHost().getHostName(), port)
  k.ks("f:{[i] 4.0 * (1 - (i mod 2) * 2) % ((2 * i) + 1)}")
  k.ks("calcPi:{[start;nelem] {[acc;i] acc+f[i]}/[f[start],1+start+til nelem]}")
  k.ks(".z.ps:{[x] h:.z.w; (neg h) value x;}")

  val broadcaster = context.actorFor("/user/webSocketBroadcaster")
  broadcaster ! WebSocketBroadcastText("worker %d created".format(port))

  def receive = {
    case Work(start, numElements) =>
      val p: Double = k.k("calcPi", start, numElements)
      broadcaster ! WebSocketBroadcastText("worker %d p: %.6f".format(port, p))
      sender ! Result(p) // perform the work
    //      k.ks("calcPi", start, numElements)
    //      self ! Wait

    case Wait =>
      val p: Double = k.k()
      println("worker %d p: %.6f".format(port, p))
      broadcaster ! WebSocketBroadcastText("worker %d p: %.6f".format(port, p))
      sender ! Result(p)

    case Shutdown =>
      context.stop(self)
      k.ks("exit 0") // end q
      q.exitValue
  }
}
