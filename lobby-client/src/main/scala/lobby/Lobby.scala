package lobby

import core.{GameStatus, PublicGameInfo, Username}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import lobby.components.ChatComponent
import org.scalajs.dom
import org.scalajs.dom.{WebSocket, document, html}
import websocket._

object Lobby {
  def run(): Unit = {
    val urlInput = document.getElementById("data-url").asInstanceOf[html.Input]
    val url = urlInput.value

    val socket = new WebSocket(url)

    val userList = document.getElementById("lobby-user-list").asInstanceOf[html.UList]
    val gameList = document.getElementById("lobby-game-list").asInstanceOf[html.UList]

    val chatComponent = new ChatComponent("chat-area", "lobby-message-input", { value =>
      socket.send(ClientLobbyChatMessage(value).asInstanceOf[ClientWebSocketMessage].asJson.noSpaces)
    })

    socket.onmessage = { event =>
      val jsonData = decode[ServerWebSocketMessage](event.data.toString)

      jsonData match {
        case Left(error) => println(s"Error $error decoding ${event.data}")
        case Right(ServerLobbyChatMessage(None, content)) =>
          chatComponent.addMessage(s"<b>$content</b>")
        case Right(ServerLobbyChatMessage(Some(Username(sender)), content)) =>
          chatComponent.addMessage(s"<b>$sender:</b> $content")
        case Right(ServerUpdatedLobbyUsers(users)) =>
          userList.innerHTML = ""
          users.foreach { user =>
            userList.innerHTML += s"<li>$user</li>"
          }
        case Right(LobbyGameList(games)) =>
          gameList.innerHTML = ""
          val canJoin = !games.filter(_.status == GameStatus.WaitingToStart).exists(_.isUserIn == true)
          games.filter(_.status != GameStatus.Finished).foreach { case PublicGameInfo(gameId, name, maxPlayerCount, hasPassword, playerCount, status, isUserIn) =>
            val li = document.createElement("li").asInstanceOf[html.LI]
            val btn = document.createElement(s"button").asInstanceOf[html.Button]

            val (href, btnText) = if (isUserIn && status == GameStatus.WaitingToStart) {
              (s"/partylobby/$gameId", "To party lobby")
            } else if (isUserIn && status == GameStatus.Running) {
              (s"/samurai?gameId=$gameId", "Back to game")
            } else {
              (s"/joinGame?gameId=$gameId", "Join")
            }

            btn.onclick = { _ =>
              document.location.href = href
            }

            btn.innerText = btnText
            li.innerHTML = s"<li><b>$name</b>\tPlayers: $playerCount/$maxPlayerCount\tHas password? ${if (hasPassword) "Yes" else "No"}</li>"
            if (isUserIn || canJoin) { //TODO: Do this logic on the server side as well
              li.appendChild(btn)
            }
            gameList.appendChild(li)
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
