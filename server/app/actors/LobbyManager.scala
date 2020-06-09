package actors

import actors.LobbyManager._
import akka.actor.{Actor, ActorRef}
import akka.pattern.pipe
import core.{PublicGameInfo, Username}
import websocket._

import scala.concurrent.{ExecutionContext, Future}

//TODO: Split into LobbyManager and GameManager
class LobbyManager extends Actor {

  private var usersMap = Map.empty[Username, Set[ActorRef]]

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
    case PartyChatMessage(gameId, sender, content, recipients) =>
      recipients.flatMap(r => usersMap(r)).foreach(_ ! WebSocketActor.SendToClient(ServerPartyChatMessage(Some(sender), gameId, content)))
    case ClientMessageReceived(sender, ClientLobbyChatMessage(content)) =>
      usersMap.values.flatten.foreach(_ ! WebSocketActor.SendToClient(ServerLobbyChatMessage(Some(sender), content)))
    case InternalLobbyMessage(message) =>
      usersMap.values.flatten.foreach(_ ! WebSocketActor.SendToClient(message))
    case GameListChanged(games) =>
      usersMap.values.flatten.foreach(_ ! WebSocketActor.SendToClient(LobbyGameList(games)))
    case m => println(s"Unknown message sent to LobbyManager $m")
  }
}

object LobbyManager {
  case class RequestOnlineUserList()
  case class NewUser(username: Username, actor: ActorRef)
  case class UserLeft(username: Username, actor: ActorRef)
  case class PartyLobbyUsersChanged(gameId: Int)
  case class ClientMessageReceived(sender: Username, message: ClientWebSocketMessage)
  case class InternalLobbyMessage(message: ServerWebSocketMessage)
  case class InternalPartyMessage(gameId: Int, message: ServerWebSocketMessage)
  case class PartyChatMessage(gameId: Int, sender: Username, content: String, recipients: Seq[Username])

  case class GameListChanged(games: Seq[PublicGameInfo])
}