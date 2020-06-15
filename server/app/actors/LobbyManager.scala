package actors

import actors.LobbyManager._
import actors.WebSocketActor.SendToClient
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import core.{GameInfo, PublicGameInfo, Username}
import websocket._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class LobbyManager(gameManager: ActorRef) extends Actor with ActorLogging {

  private var usersMap = Map.empty[Username, Set[ActorRef]]
  private implicit val defaultTimeout: Timeout = Timeout(5.seconds)
  private implicit val ec: ExecutionContextExecutor = context.dispatcher

  override def receive: Receive = {
    case RequestOnlineUserList() =>
      sender() ! usersMap.keys
    case NewUser(username, actor) =>
      val existingActors = usersMap.getOrElse(username, Set())
      usersMap = usersMap + (username -> (existingActors + actor))
      if (existingActors.isEmpty) {
        self ! InternalLobbyMessage(ServerLobbyChatMessage(None, s"${username.value} joined the lobby"))
        self ! InternalLobbyMessage(ServerUpdatedLobbyUsers(usersMap.keys.map(_.value).toSeq))
      }
      (gameManager ? GameManager.GetGames).mapTo[Seq[GameInfo]].map(games => SendToClient(LobbyGameList(games.map(PublicGameInfo(_, username))))).pipeTo(actor)
    case UserLeft(username, actor) =>
      val updatedActors = usersMap.get(username).map(_ - actor).getOrElse(Set.empty[ActorRef])
      if (updatedActors.isEmpty) {
        usersMap = usersMap - username
        self ! InternalLobbyMessage(ServerLobbyChatMessage(None, s"${username.value} left the lobby"))
        self ! InternalLobbyMessage(ServerUpdatedLobbyUsers(usersMap.keys.map(_.value).toSeq))
      } else {
        usersMap = usersMap + (username -> updatedActors)
      }
      (gameManager ? GameManager.GetGames).mapTo[Seq[GameInfo]].map(games => GameListChanged(games)).pipeTo(actor)
    case PartyChatMessage(gameId, sender, content, recipients) =>
      recipients.flatMap(r => usersMap(r)).foreach(_ ! WebSocketActor.SendToClient(ServerPartyChatMessage(Some(sender), gameId, content)))
    case ClientMessageReceived(sender, ClientLobbyChatMessage(content)) =>
      usersMap.values.flatten.foreach(_ ! WebSocketActor.SendToClient(ServerLobbyChatMessage(Some(sender), content)))
    case InternalLobbyMessage(message) =>
      usersMap.values.flatten.foreach(_ ! WebSocketActor.SendToClient(message))
    case GameListChanged(games) =>
      usersMap.foreach {
        case (user, actors) => actors.foreach(_ ! WebSocketActor.SendToClient(LobbyGameList(games.map(PublicGameInfo(_, user)))))
      }
    case m => log.error(s"Unknown message sent to LobbyManager $m")
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

  case class GameListChanged(games: Seq[GameInfo])
}