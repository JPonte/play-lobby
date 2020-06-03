package lobby

import core.{LobbyMessage, SystemLobbyMessage, UpdatedUsersList, Username, WebSocketCommand}
import org.scalajs.dom
import org.scalajs.dom.{WebSocket, document, html}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._


object MainApp {
  def main(args: Array[String]): Unit = {
    val urlInput = document.getElementById("data-url").asInstanceOf[html.Input]
    val url = urlInput.value

    val socket = new WebSocket(url);


    val inputField = document.getElementById("lobby-message-input").asInstanceOf[html.Input]
    val userList = document.getElementById("lobby-user-list").asInstanceOf[html.UList]
    val chatArea = document.getElementById("chat-area").asInstanceOf[html.Div]

    inputField.onkeydown = { event =>
      if (event.key == "Enter") {
        socket.send(inputField.value);
        inputField.value = ""
      }
    }

    socket.onmessage = { event =>
      val jsonData = decode[WebSocketCommand](event.data.toString)

      jsonData match {
        case Left(error) => println(s"Error $error decoding ${event.data}")
        case Right(SystemLobbyMessage(content)) =>
          chatArea.innerHTML = s"<div><b>$content</b></div>${chatArea.innerHTML}"
        case Right(LobbyMessage(Username(sender), content)) =>
          chatArea.innerHTML = s"<div><b>$sender:</b> $content</div>${chatArea.innerHTML}"
        case Right(UpdatedUsersList(users)) =>
          userList.innerHTML = ""
          users.foreach { user =>
            userList.innerHTML += s"<li>$user</li>"
          }
        case x => println(s"Couldn't handle $x")
      }
    }

    val modalBtn = document.getElementById("create-game-button").asInstanceOf[html.Button]
    val modal = document.querySelector(".modal").asInstanceOf[html.Element]
    val closeBtn = document.querySelector(".close-btn").asInstanceOf[html.Element]
    modalBtn.onclick = { _ =>
      modal.style.display = "block"
    }
    closeBtn.onclick = { _ =>
      modal.style.display = "none"
    }
    dom.window.onclick = { e =>
      if (e.target == modal) {
        modal.style.display = "none"
      }
    }
  }
}