package websocket

import core.{GameInfo, PublicGameInfo, Username}
import samurai.GameState

sealed trait ServerWebSocketMessage

case class ServerLobbyChatMessage(sender: Option[Username], content: String) extends ServerWebSocketMessage
case class ServerPartyChatMessage(sender: Option[Username], gameId: Int, content: String) extends ServerWebSocketMessage

case class ServerUpdatedLobbyUsers(userList: Seq[String]) extends ServerWebSocketMessage
case class ServerUpdatedPartyUsers(gameId: Int, userList: Seq[String]) extends ServerWebSocketMessage

case class LobbyGameList(games: Seq[PublicGameInfo]) extends ServerWebSocketMessage
case class InvalidGameMove(message: String) extends ServerWebSocketMessage

case class UpdatedGameState(gameInfo: GameInfo, gameState: Option[GameState]) extends ServerWebSocketMessage