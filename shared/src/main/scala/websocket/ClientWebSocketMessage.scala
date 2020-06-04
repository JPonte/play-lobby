package websocket

sealed trait ClientWebSocketMessage

case class ClientLobbyChatMessage(content: String) extends ClientWebSocketMessage
case class ClientPartyChatMessage(gameId: Int, content: String) extends ClientWebSocketMessage