package lobby

import core.Username
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import lobby.components.ChatComponent
import org.scalajs.dom.{WebSocket, document, html}
import websocket._

object Party {
  def run(): Unit ={
    val gameId = document.getElementById("game-id").asInstanceOf[html.Input].value.toInt
    val url = document.getElementById("data-url").asInstanceOf[html.Input].value

    val socket = new WebSocket(url)

    val userList = document.getElementById("party-player-list").asInstanceOf[html.Div]

    val chatComponent = new ChatComponent("chat-area", "lobby-message-input", { value =>
      socket.send(ClientPartyChatMessage(gameId, value).asInstanceOf[ClientWebSocketMessage].asJson.noSpaces)
    })

    socket.onmessage = { event =>
      val jsonData = decode[ServerWebSocketMessage](event.data.toString)

      jsonData match {
        case Left(error) => println(s"Error $error decoding ${event.data}")
        case Right(ServerPartyChatMessage(None, `gameId`, content)) =>
          chatComponent.addMessage(s"<b>$content</b>")
        case Right(ServerPartyChatMessage(Some(Username(sender)), `gameId`, content)) =>
          chatComponent.addMessage(s"<b>$sender:</b> $content")
        case Right(ServerUpdatedPartyUsers(`gameId`, users)) =>
          userList.innerHTML = ""
          users.foreach { user =>
            userList.innerHTML += s"<div class='waiting-player'>$user</div>"
          }
        case x => println(s"Couldn't handle $x")
      }
    }
  }
}
