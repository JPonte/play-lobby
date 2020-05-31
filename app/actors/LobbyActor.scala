package actors

import actors.LobbyActor.SendCommand
import akka.actor.{Actor, ActorRef, Props}
import models.{LobbyMessage, SystemLobbyMessage, Username, WebSocketCommand}

class LobbyActor(username: Username, out: ActorRef, manager: ActorRef) extends Actor {

  manager ! LobbyManager.NewUser(username, self)

  override def receive: Receive = {
    case message: String => manager ! LobbyManager.Command(LobbyMessage(username, message))
    case SendCommand(LobbyMessage(sender, content)) => out ! s"${sender.value}: $content"
    case SendCommand(SystemLobbyMessage(content)) => out ! content
    case m => println(s"Unhandled message: $m")
  }

  override def postStop(): Unit = {
    manager ! LobbyManager.UserLeft(username, self)
  }

}

object LobbyActor {
  def props(username: Username, out: ActorRef, manager: ActorRef): Props = Props(new LobbyActor(username, out, manager))

  case class SendCommand(message: WebSocketCommand)
}