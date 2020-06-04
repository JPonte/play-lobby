package actors

import actors.LobbyManager._
import akka.actor.{Actor, ActorRef}
import akka.pattern.pipe
import core.Username
import websocket._

import scala.concurrent.{ExecutionContext, Future}

class LobbyManager extends Actor {

  private var usersMap = Map.empty[Username, Set[ActorRef]]
  private var gameActors = Map.empty[Int, ActorRef]

  override def receive: Receive = {
    case RequestOnlineUserList() =>
      implicit val ec: ExecutionContext = context.dispatcher
      Future(usersMap.keys).pipeTo(sender())
    case NewUser(username, actor) =>
      val existingActors = usersMap.getOrElse(username, Set())
      usersMap = usersMap + (username -> (existingActors + actor))
      if (existingActors.isEmpty) {
        self ! InternalLobbyMessage(ServerLobbyChatMessage(None, s"${username.value} joined the lobby"))
        self ! InternalLobbyMessage(ServerUpdatedLobbyUsers(usersMap.keys.map(_.value).toSeq))
      }
    case UserLeft(username, actor) =>
      val updatedActors = usersMap.get(username).map(_ - actor).getOrElse(Set.empty[ActorRef])
      if (updatedActors.isEmpty) {
        usersMap = usersMap - username
        self ! InternalLobbyMessage(ServerLobbyChatMessage(None, s"${username.value} left the lobby"))
        self ! InternalLobbyMessage(ServerUpdatedLobbyUsers(usersMap.keys.map(_.value).toSeq))
      } else {
        usersMap = usersMap + (username -> updatedActors)
      }
    case GameCreated(gameId, actor) =>
      gameActors = gameActors + (gameId -> actor)
      //TODO: ask the game actor for the users
      self ! InternalLobbyMessage(ServerUpdatedPartyUsers(gameId, Seq()))
    case GameFinished(gameId) =>
      gameActors = gameActors - gameId
      //TODO: ask the game actor for the users
      self ! InternalLobbyMessage(ServerUpdatedPartyUsers(gameId, Seq()))
    case ClientMessageReceived(sender, ClientLobbyChatMessage(content)) =>
      usersMap.values.flatten.foreach(_ ! LobbyActor.SendToClient(ServerLobbyChatMessage(Some(sender), content)))
    case InternalLobbyMessage(message) =>
      usersMap.values.flatten.foreach(_ ! LobbyActor.SendToClient(message))
    case m => println(s"Unknown message sent to LobbyManager $m")
  }
}

object LobbyManager {
  case class RequestOnlineUserList()
  case class NewUser(username: Username, actor: ActorRef)
  case class UserLeft(username: Username, actor: ActorRef)
  case class GameCreated(gameId: Int, actor: ActorRef)
  case class GameFinished(gameId: Int)
  case class PartyLobbyUsersChanged(gameId: Int)
  case class ClientMessageReceived(sender: Username, message: ClientWebSocketMessage)
  case class InternalLobbyMessage(message: ServerWebSocketMessage)
  case class InternalPartyMessage(gameId: Int, message: ServerWebSocketMessage)
}