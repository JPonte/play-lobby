package actors

import actors.GameActor._
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import core.{GameInfo, GameSettings, GameStatus, Username}
import websocket.{ClientPartyChatMessage, GameMessage, ServerPartyChatMessage}

class GameActor(gameId: Int, settings: GameSettings) extends Actor with ActorLogging {

  log.debug(s"GameActor for game $gameId started")

  private var gameInfo = GameInfo(gameId, 2, settings.password, Seq(), GameStatus.WaitingToStart)
  private var webSocketActors = Map.empty[Username, Set[ActorRef]]

  override def receive: Receive = {
    case JoinGame(username) =>
      gameInfo = gameInfo.copy(players = (gameInfo.players ++ Seq(username)).distinct)
      sender() ! gameId
      log.debug(s"User $username joined the game #$gameId")
    case LeaveGame(username) =>
      gameInfo = gameInfo.copy(players = gameInfo.players.filter(_ != username))
      sender() ! true
      log.debug(s"User $username left the game #$gameId")
    case UserConnected(username, actor) =>
      if (gameInfo.players.contains(username)) {
        val existingActors = webSocketActors.getOrElse(username, Set())
        webSocketActors = webSocketActors + (username -> (existingActors + actor))
        actor ! GameWebSocketActor.ConnectToGame(gameId, self)
        log.debug(s"User $username connected")
      } else {
        log.debug(s"User $username tried to connect but isn't part of the game")
      }
    case UserDisconnected(username, actor) =>
      val updatedActors = webSocketActors.get(username).map(_ - actor).getOrElse(Set.empty[ActorRef])
      actor ! GameWebSocketActor.DisconnectFromGame(gameId)
      log.debug(s"User $username disconnected")
      if (updatedActors.isEmpty) {
        webSocketActors = webSocketActors - username
      } else {
        webSocketActors = webSocketActors + (username -> updatedActors)
      }
    case ProcessGameMessage(username, ClientPartyChatMessage(gameId, content)) =>
      log.debug(content)
      webSocketActors.flatMap(_._2)
        .foreach { actor =>
          log.debug(actor.toString())
          actor ! GameWebSocketActor.SendToClient(ServerPartyChatMessage(Some(username), gameId, content))
        }

    case GetGameInfo =>
      sender() ! gameInfo
    case _ =>
  }
}

object GameActor {
  case class ProcessGameMessage(username: Username, m : GameMessage)

  case class JoinGame(username: Username)
  case class LeaveGame(username: Username)

  case class UserConnected(username: Username, actor: ActorRef)
  case class UserDisconnected(username: Username, actor: ActorRef)

  case object GetGameInfo

  def props(gameId: Int, settings: GameSettings): Props = Props(new GameActor(gameId, settings))
}