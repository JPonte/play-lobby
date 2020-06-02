package actors

import actors.LobbyActor.SendCommand
import akka.actor.{Actor, ActorRef, Props}
import io.circe.generic.auto._
import io.circe.syntax._
import models.{LobbyMessage, Username, WebSocketCommand}

class LobbyActor(username: Username, out: ActorRef, manager: ActorRef) extends Actor {

  manager ! LobbyManager.NewUser(username, self)

  override def receive: Receive = {
    case message: String => manager ! LobbyManager.Command(LobbyMessage(username, message))
    case SendCommand(command) => out ! command.asJson.spaces2
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