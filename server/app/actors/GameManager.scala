package actors

import actors.GameManager._
import actors.LobbyManager.ClientMessageReceived
import akka.actor.{Actor, ActorRef}
import core.Username
import websocket.{ClientPartyChatMessage, GameMessage, ServerUpdatedPartyUsers}

class GameManager(lobbyManager: ActorRef) extends Actor {

  private var gameActors = Map.empty[Int, ActorRef]

  override def receive: Receive = {
    case GameCreated(gameId, actor) =>
      gameActors = gameActors + (gameId -> actor)
    case GameFinished(gameId) =>
      gameActors = gameActors - gameId
    case ClientMessageReceived(sender, ClientPartyChatMessage(gameId, content)) =>
      gameActors.get(gameId).foreach(_ ! GameActor.ChatMessage(sender, content, self))

    case NewUser(username, gameId, actor) =>
      gameActors.get(gameId).foreach(_ ! GameActor.NewUser(username, actor))
    case UserLeft(username, gameId, actor) =>
      gameActors.get(gameId).foreach(_ ! GameActor.UserLeft(username, actor))
    case m => println(s"${this.getClass}: Unhandled message $m")
  }
}

object GameManager {
  case class GameCreated(gameId: Int, actor: ActorRef)
  case class GameFinished(gameId: Int)

  case class GameMessageReceived(message: GameMessage)

  case class NewUser(username: Username, gameId: Int, actor: ActorRef)
  case class UserLeft(username: Username, gameId: Int, actor: ActorRef)
}