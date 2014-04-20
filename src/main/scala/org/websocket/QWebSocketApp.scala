package org.websocket

import akka.actor.{ ActorSystem, Props, Actor }
import akka.event.Logging
import org.mashupbots.socko.routes._
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.events.WebSocketFrameEvent
import org.mashupbots.socko.handlers.WebSocketBroadcastText
import org.mashupbots.socko.handlers.WebSocketBroadcaster
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import Messages._
import org.mashupbots.socko.handlers.WebSocketBroadcasterRegistration

/**
 * Web Socket processor that echos incoming text frames in upper case.
 */
class WebSocketHandler extends Actor {
  val log = Logging(context.system, this)

  // Process incoming messages
  def receive = {
    case event: HttpRequestEvent =>
      // Return the HTML page to setup web sockets in the browser
      writeHTML(event)
      context.stop(self)
    case event: WebSocketFrameEvent =>
      // Echo web socket text frames
      writeWebSocketResponse(event)
      context.stop(self)
    case _ => {
      log.info("received unknown message of type: ")
      context.stop(self)
    }
  }

  // Write HTML page to setup a web socket on the browser
  private def writeHTML(ctx: HttpRequestEvent) {
    // Send 100 continue if required
    if (ctx.request.is100ContinueExpected) {
      ctx.response.write100Continue()
    }

    val html =
      """<html><head><title>Socko Web Socket Example</title></head>
		<body>
		<script type="text/javascript">
		  var socket;
		  if (!window.WebSocket) {
    		window.WebSocket = window.MozWebSocket;
		  }
		  if (window.WebSocket) {
			socket = new WebSocket("ws://localhost:8888/websocket/"); // Note the address must match the route
			socket.onmessage = function(event) { 
    			var ta = document.getElementById('responseText'); 
    			ta.value = ta.value + '\n' + event.data 
    		};
			socket.onopen = function(event) { 
    			var ta = document.getElementById('responseText'); 
    			ta.value = "Web Socket opened!"; 
    		};
			socket.onclose = function(event) { 
    			var ta = document.getElementById('responseText'); 
    			ta.value = ta.value + "Web Socket closed"; };
		  } else { 
    		alert("Your browser does not support Web Sockets.");
		  }
		  
		  function send(message) {
			if (!window.WebSocket) { return; }
			if (socket.readyState == WebSocket.OPEN) {
			  socket.send(message);
			} else {
			  alert("The socket is not open.");
			}
		  }
		</script>
		<h1>Socko Web Socket Example</h1>
		<form onsubmit="return false;">
		  <input type="text" name="message" value="Hello, World!"/>
		  <input type="button" value="Send Web Socket Data" onclick="send(this.form.message.value)" />
		  
		  <h3>Output</h3>
		  <textarea id="responseText" style="width: 500px; height:300px;"></textarea>
		</form>
		</body>
		</html>"""

    ctx.response.write(html, "text/html; charset=UTF-8")
  }

  /**
   * Echo the details of the web socket frame that we just received; but in upper case.
   */
  private def writeWebSocketResponse(event: WebSocketFrameEvent) {
    log.info("TextWebSocketFrame: " + event.readText)

    val dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val time = new GregorianCalendar()
    val ts = dateFormatter.format(time.getTime())

    //    event.channel.write(ts + " " + event.readText.toUpperCase())
    val broadcaster = context.actorFor("/user/webSocketBroadcaster")
    broadcaster ! WebSocketBroadcastText(ts + " " + event.readText)

  }
}

/**
 * This example shows how to use web sockets with Socko.
 *  - Open your browser and navigate to `http://localhost:8888/html`.
 *  - A HTML page will be displayed
 *  - It will make a web socket connection to `ws://localhost:8888/websocket/`
 *
 * This is a port of the Netty project web socket server example.
 */
object QWebSocketApp extends Logger {

  // STEP #1 - Define Actors and Start Akka
  val actorSystem = ActorSystem("WebSocketExampleActorSystem")
  val webSocketBroadcaster = actorSystem.actorOf(Props[WebSocketBroadcaster], "webSocketBroadcaster")

  val (numWorkers, numElements, numMessages) = (10, 1000, 1000)

  // create the result listener, which will print the result and shutdown the system
  // val listener = actorSystem.actorOf(Props[Listener], name = "listener")
  val listener = actorSystem.actorOf(Props(new Actor {
    def receive = {
      case PiApproximation(pi, duration) ⇒
        println("Calculated pi: %.6g in %s".format(pi, duration))
        val broadcaster = context.actorFor("/user/webSocketBroadcaster")
        broadcaster ! WebSocketBroadcastText("Calculated pi: %.6g in %s".format(pi, duration))
    }
  }))

  // create the master
  val master = actorSystem.actorOf(Props(new Master(numWorkers, numMessages, numElements, listener)), name = "master")

  // STEP #2 - Define Routes
  val routes = Routes({
    case HttpRequest(httpRequest) => httpRequest match {
      case GET(Path("/html")) => {
        // Return HTML page to establish web socket
        actorSystem.actorOf(Props[WebSocketHandler]) ! httpRequest
      }
      case Path("/favicon.ico") => {
        // If favicon.ico, just return a 404 because we don't have that file
        httpRequest.response.write(HttpResponseStatus.NOT_FOUND)
      }
    }

    case WebSocketHandshake(wsHandshake) => wsHandshake match {
      case Path("/websocket/") => {
        // For WebSocket processing, we first have to authorize the handshake by setting the "isAllowed" property.
        // This is a security measure to make sure that web sockets can only be established at your specified end points.
        wsHandshake.authorize()

        // Register this connection with the broadcaster
        webSocketBroadcaster ! new WebSocketBroadcasterRegistration(wsHandshake)
      }
    }

    case WebSocketFrame(wsFrame) => {
      // Once handshaking has taken place, we can now process frames sent from the client
      actorSystem.actorOf(Props[WebSocketHandler]) ! wsFrame
      master ! Calculate
    }
  })

  // STEP #3 - Start and Stop Socko Web Server
  def main(args: Array[String]) {
    val webServer = new WebServer(WebServerConfig(), routes, actorSystem)
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })
    webServer.start()

    System.out.println("Open your browser and navigate to http://localhost:8888/html")
  }
}