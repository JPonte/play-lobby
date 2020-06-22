package samurai

import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, Event, WebSocket}

import scala.util.{Failure, Success, Try}

class WebSocketWrapper(url: String, onMessage: WebSocket => MessageEvent => (), onOpen: WebSocket => Event => ()) {

  private var socket = Option.empty[WebSocket]

  initSocket()

  private def onClose(event: CloseEvent): Unit = {
    println(s"WebSocket closed ($event)")
    socket = None
    initSocket()
  }

  private def initSocket(): Unit = {

    val newWebSocket = Try(new WebSocket(url))

    newWebSocket match {
      case Failure(exception) => println(exception.getMessage)
      case Success(websocket) =>
        websocket.onopen = { event =>
          println("WebSocket opened")
          onOpen(websocket)(event)
          socket = Some(websocket)
        }

        websocket.onclose = onClose

        websocket.onmessage = onMessage(websocket)
    }
  }

  def send(message: String): Unit = {
    socket.foreach(_.send(message))
  }
}
