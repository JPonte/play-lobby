package websocket

sealed trait ClientWebSocketMessage

sealed trait LobbyMessage
sealed trait GameMessage {
  def gameId: Int
}

case class ClientLobbyChatMessage(content: String) extends ClientWebSocketMessage with LobbyMessage
case class ClientPartyChatMessage(gameId: Int, content: String) extends ClientWebSocketMessage with GameMessage

case class ClientSamuraiGameMove(gameId: Int, gameMove: samurai.GameMove)