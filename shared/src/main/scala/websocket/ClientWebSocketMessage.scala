package websocket

sealed trait ClientWebSocketMessage

sealed trait LobbyMessage
sealed trait GameMessage

case class ClientLobbyChatMessage(content: String) extends ClientWebSocketMessage with LobbyMessage
case class ClientPartyChatMessage(gameId: Int, content: String) extends ClientWebSocketMessage with GameMessage

case class ClientSamuraiGameMove(gameMove: samurai.GameMove) extends ClientWebSocketMessage with GameMessage
case object ClientRequestGameState extends ClientWebSocketMessage with GameMessage