package actors

import actors.WebSocketActor._
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import core.Username
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import websocket._

class WebSocketActor(username: Username, out: ActorRef, lobbyManager: ActorRef) extends Actor with Stash with ActorLogging {

  lobbyManager ! LobbyManager.NewUser(username, self)

  override def receive: Receive = {
    case message: String =>
      decode[ClientWebSocketMessage](message) match {
        case Right(m: LobbyMessage) =>
          lobbyManager ! LobbyManager.ClientMessageReceived(username, m)
        case _ =>
          log.error(s"Invalid message $message")
      }
    case SendToClient(message) => out ! message.asJson.spaces2
    case m =>
      log.error(s"Unhandled message: $m")
  }

  override def postStop(): Unit = {
    lobbyManager ! LobbyManager.UserLeft(username, self)
  }

}

object WebSocketActor {
  def props(username: Username, out: ActorRef, lobbyManager: ActorRef): Props = Props(new WebSocketActor(username, out, lobbyManager))

  case class SendToClient(message: ServerWebSocketMessage)
}