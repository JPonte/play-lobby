package actors

import actors.LobbyActor.SendToClient
import akka.actor.{Actor, ActorRef, Props}
import core.Username
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import websocket._

class LobbyActor(username: Username, out: ActorRef, manager: ActorRef) extends Actor {

  manager ! LobbyManager.NewUser(username, self)

  override def receive: Receive = {
    case message: String =>
      decode[ClientWebSocketMessage](message).toOption.foreach(c => manager ! LobbyManager.ClientMessageReceived(username, c))
    case SendToClient(message) => out ! message.asJson.spaces2
    case m => println(s"Unhandled message: $m")
  }

  override def postStop(): Unit = {
    manager ! LobbyManager.UserLeft(username, self)
  }

}

object LobbyActor {
  def props(username: Username, out: ActorRef, manager: ActorRef): Props = Props(new LobbyActor(username, out, manager))

  case class SendToClient(message: ServerWebSocketMessage)
}