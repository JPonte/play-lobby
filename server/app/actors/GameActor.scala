package actors

import actors.GameActor._
import actors.GameWebSocketActor.SendToClient
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import core.{GameInfo, GameSettings, GameStatus, Username}
import samurai.GameState
import websocket.{ClientPartyChatMessage, ClientRequestGameState, ClientSamuraiGameMove, GameMessage, InvalidGameMove, ServerPartyChatMessage, ServerUpdatedPartyUsers, ServerWebSocketMessage, UpdatedGameState}

class GameActor(gameId: Int, settings: GameSettings) extends Actor with ActorLogging {

  log.debug(s"GameActor for game $gameId started")

  private var gameInfo = GameInfo(gameId, 2, settings.password, Seq(), GameStatus.WaitingToStart)
  private var gameState = Option.empty[GameState]

  private var webSocketActors = Map.empty[Username, Set[ActorRef]]

  override def receive: Receive = {
    case JoinGame(username) =>
      gameInfo = gameInfo.copy(players = (gameInfo.players ++ Seq(username)).distinct)
      sender() ! gameId
      notifyGameInfoChanged()
      log.debug(s"User $username joined the game #$gameId")
    case LeaveGame(username) =>
      gameInfo = gameInfo.copy(players = gameInfo.players.filter(_ != username))
      sender() ! true
      notifyGameInfoChanged()
      log.debug(s"User $username left the game #$gameId")

    case StartGame =>
      if (gameInfo.status == GameStatus.WaitingToStart && gameInfo.players.size == gameInfo.playerCount) {
        gameInfo = gameInfo.copy(status = GameStatus.Running)
        gameState = Some(GameState.initialGameState(gameInfo.players, addFiguresAuto = true))
        notifyGameStateChanged()
        sender() ! true
      } else {
        log.error(s"Invalid attempt to start the game on $gameInfo")
        sender() ! false
      }
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
    case ProcessGameMessage(username, ClientSamuraiGameMove(move)) =>
      gameState match {
        case Some(value) =>
          val moveResult = value.play(move)
          if (moveResult.isEmpty) {
            notifyClient(username, InvalidGameMove("Invalid move"))
          } else {
            gameState = moveResult
            notifyGameStateChanged()
          }
        case None =>
          log.error("Game move before game has started")
          notifyClient(username, InvalidGameMove("Game hasn't started yet."))
      }
    case ProcessGameMessage(username, ClientRequestGameState) =>
      notifyClient(username, UpdatedGameState(gameId, gameState))
    case GetGameInfo =>
      sender() ! gameInfo
    case _ =>
  }

  def notifyClient(username: Username, message: ServerWebSocketMessage): Unit =
    webSocketActors.get(username).foreach(_.foreach(_ ! SendToClient(message)))

  def notifyAllClients(message: ServerWebSocketMessage): Unit =
    webSocketActors.values.flatten.foreach(_ ! SendToClient(message))

  def notifyGameInfoChanged(): Unit =
    notifyAllClients(ServerUpdatedPartyUsers(gameId, gameInfo.players.map(_.value)))

  def notifyGameStateChanged(): Unit =
    notifyAllClients(UpdatedGameState(gameId, gameState))
}

object GameActor {
  case class ProcessGameMessage(username: Username, m : GameMessage)
  case object StartGame

  case class JoinGame(username: Username)
  case class LeaveGame(username: Username)

  case class UserConnected(username: Username, actor: ActorRef)
  case class UserDisconnected(username: Username, actor: ActorRef)

  case object GetGameInfo

  def props(gameId: Int, settings: GameSettings): Props = Props(new GameActor(gameId, settings))
}