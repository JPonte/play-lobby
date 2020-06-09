package lobby

import core.{PublicGameInfo, Username}
import org.scalajs.dom
import org.scalajs.dom.{WebSocket, document, html}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import websocket.{ClientLobbyChatMessage, ClientWebSocketMessage, LobbyGameList, ServerLobbyChatMessage, ServerUpdatedLobbyUsers, ServerWebSocketMessage}

object Lobby {
  def run(): Unit = {
    val urlInput = document.getElementById("data-url").asInstanceOf[html.Input]
    val url = urlInput.value

    val socket = new WebSocket(url)

    val userList = document.getElementById("lobby-user-list").asInstanceOf[html.UList]
    val gameList = document.getElementById("lobby-game-list").asInstanceOf[html.UList]

    val chatArea = document.getElementById("chat-area").asInstanceOf[html.Div]
    val inputField = document.getElementById("lobby-message-input").asInstanceOf[html.Input]
    inputField.onkeydown = { event =>
      if (event.key == "Enter") {
        socket.send(ClientLobbyChatMessage(inputField.value).asInstanceOf[ClientWebSocketMessage].asJson.noSpaces)
        inputField.value = ""
      }
    }

    socket.onmessage = { event =>
      val jsonData = decode[ServerWebSocketMessage](event.data.toString)

      jsonData match {
        case Left(error) => println(s"Error $error decoding ${event.data}")
        case Right(ServerLobbyChatMessage(None, content)) =>
          chatArea.innerHTML = s"<div><b>$content</b></div>${chatArea.innerHTML}"
        case Right(ServerLobbyChatMessage(Some(Username(sender)), content)) =>
          chatArea.innerHTML = s"<div><b>$sender:</b> $content</div>${chatArea.innerHTML}"
        case Right(ServerUpdatedLobbyUsers(users)) =>
          userList.innerHTML = ""
          users.foreach { user =>
            userList.innerHTML += s"<li>$user</li>"
          }
        case Right(LobbyGameList(games)) =>
          println(games)
          gameList.innerHTML = ""
          games.foreach { case PublicGameInfo(gameId, maxPlayerCount, hasPassword, playerCount, status) =>
            val btn = document.createElement("button").asInstanceOf[html.Button]
            btn.onclick = { e =>
              document.location.href = s"/joinGame?gameId=$gameId"
            }
            btn.innerText = "Join"
            gameList.innerHTML += s"<li>$gameId\t$playerCount/$maxPlayerCount\tPassword: $hasPassword\t$status\t</li>"
            gameList.appendChild(btn)
          }
        case x => println(s"Couldn't handle $x")
      }
    }

    //TODO: make it a reusable modal component
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
