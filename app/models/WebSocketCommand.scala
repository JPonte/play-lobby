package models

trait WebSocketCommand

trait LobbyCommand

trait PrivateCommand {
  def recipient: Username
}

case class LobbyMessage(sender: Username, content: String) extends WebSocketCommand with LobbyCommand
case class SystemLobbyMessage(content: String) extends WebSocketCommand with LobbyCommand

case class PrivateMessage(sender: Username, recipient: Username, content: String) extends WebSocketCommand with PrivateCommand