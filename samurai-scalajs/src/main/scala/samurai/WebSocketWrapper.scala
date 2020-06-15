package samurai

import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, Event, WebSocket}

class WebSocketWrapper(url: String, onMessage: WebSocket => MessageEvent => (), onOpen: WebSocket => Event => ()) {

  private var socket = Option.empty[WebSocket]

  initSocket()

  private def onClose(event: CloseEvent): Unit = {
    println(s"WebSocket closed ($event)")
    socket = None
    initSocket()
  }

  private def initSocket(): Unit = {

    val newWebSocket = new WebSocket(url)

    newWebSocket.onopen = { event =>
      println("WebSocket opened")
      onOpen(newWebSocket)(event)
      socket = Some(newWebSocket)
    }

    newWebSocket.onclose = onClose

    newWebSocket.onmessage = onMessage(newWebSocket)
  }

  def send(message: String): Unit = {
    socket.foreach(_.send(message))
  }
}
