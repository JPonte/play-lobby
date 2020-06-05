package lobby.components

import org.scalajs.dom.{WebSocket, document, html}
import org.scalajs.dom.html.{Div, Input}

class ChatComponent(chatAreaId: String, inputFieldId: String, callback: String => Unit) {

  val chatArea: Div = document.getElementById(chatAreaId).asInstanceOf[html.Div]
  val inputField: Input = document.getElementById(inputFieldId).asInstanceOf[html.Input]

  inputField.onkeydown = { event =>
    if (event.key == "Enter") {
      callback(inputField.value)
      inputField.value = ""
    }
  }

  def addMessage(content: String): Unit = {
    chatArea.innerHTML = s"<div>$content</div>${chatArea.innerHTML}"
  }
}
