package actors

import actors.WebSocketActor._
import akka.actor.{Actor, ActorRef, Props, Stash}
import core.Username
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import websocket._

class WebSocketActor(username: Username, out: ActorRef, lobbyManager: ActorRef, gameManager: ActorRef) extends Actor with Stash {

  lobbyManager ! LobbyManager.NewUser(username, self)

  private var gameActors = Map.empty[Int, ActorRef]

  override def receive: Receive = {
    case message: String =>
      decode[ClientWebSocketMessage](message) match {
        case Right(m: LobbyMessage) =>
          lobbyManager ! LobbyManager.ClientMessageReceived(username, m)
        case Right(m: GameMessage) =>
          gameActors.get(m.gameId) match {
            case Some(gameActor) => gameActor ! GameActor.ProcessGameMessage(username, m)
            case None => stash()
          }
          gameManager ! LobbyManager.ClientMessageReceived(username, m)
      }
    case SendToClient(message) => out ! message.asJson.spaces2
    case JoinGame(gameId: Int, actorRef: ActorRef) =>
      gameManager ! GameManager.NewUser(username, gameId, self)
      gameActors += gameId -> actorRef
      unstashAll()
    case LeaveGame(gameId: Int) =>
      gameManager ! GameManager.UserLeft(username, gameId, self)
      gameActors -= gameId
    case m => println(s"Unhandled message: $m")
  }

  override def postStop(): Unit = {
    lobbyManager ! LobbyManager.UserLeft(username, self)
  }

}

object WebSocketActor {
  def props(username: Username, out: ActorRef, lobbyManager: ActorRef, gameManager: ActorRef): Props = Props(new WebSocketActor(username, out, lobbyManager, gameManager))

  case class SendToClient(message: ServerWebSocketMessage)
  case class JoinGame(gameId: Int, gameActor: ActorRef)
  case class LeaveGame(gameId: Int)
}