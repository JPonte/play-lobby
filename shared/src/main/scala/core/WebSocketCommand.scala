package core

sealed trait WebSocketCommand

sealed trait LobbyCommand
sealed trait PrivateCommand {
  def recipient: Username
}
sealed trait GameCommand {
  def gameId: String
}

case class LobbyMessage(sender: Username, content: String) extends WebSocketCommand with LobbyCommand
case class SystemLobbyMessage(content: String) extends WebSocketCommand with LobbyCommand
case class UpdatedUsersList(userList: Seq[String]) extends WebSocketCommand with LobbyCommand

case class PrivateMessage(sender: Username, recipient: Username, content: String) extends WebSocketCommand with PrivateCommand