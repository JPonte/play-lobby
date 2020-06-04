package websocket

import core.Username

sealed trait ServerWebSocketMessage

case class ServerLobbyChatMessage(sender: Option[Username], content: String) extends ServerWebSocketMessage
case class ServerPartyChatMessage(sender: Option[Username], gameId: Int, content: String) extends ServerWebSocketMessage

case class ServerUpdatedLobbyUsers(userList: Seq[String]) extends ServerWebSocketMessage
case class ServerUpdatedPartyUsers(gameId: Int, userList: Seq[String]) extends ServerWebSocketMessage
