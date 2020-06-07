package actors

import actors.GameActor._
import actors.LobbyManager.PartyChatMessage
import akka.actor.{Actor, ActorRef}
import core.Username
import models.GameInfoRepository
import websocket.{ClientPartyChatMessage, GameMessage, ServerPartyChatMessage, ServerUpdatedPartyUsers}

import scala.concurrent.ExecutionContextExecutor

class GameActor(gameId: Int, gameInfoRepository: GameInfoRepository) extends Actor {

  println(s"GameActor for game $gameId started")

  //TODO: Map of sets like the lobby?
  var webSocketActors = Map.empty[Username, ActorRef]

  override def receive: Receive = {
    case ChatMessage(sender, content, actorRef) =>
      implicit val ec: ExecutionContextExecutor = context.dispatcher
      gameInfoRepository.getGameInfo(gameId).map(_.foreach(gameInfo => actorRef ! PartyChatMessage(gameId, sender, content, gameInfo.players)))
    case InGameUsers(actorRef) =>
      implicit val ec: ExecutionContextExecutor = context.dispatcher
      gameInfoRepository.getGameInfo(gameId).map(_.foreach(gameInfo => actorRef ! ServerUpdatedPartyUsers(gameId, gameInfo.players.map(_.value))))
    case NewUser(username, actorRef) =>
      webSocketActors += username -> actorRef
    case UserLeft(username, actorRef) =>
      webSocketActors -= username

    case ProcessGameMessage(username, ClientPartyChatMessage(gameId, content)) =>
      webSocketActors.foreach(_._2 ! ServerPartyChatMessage(Some(username), gameId, content))
    case _ =>
  }
}

object GameActor {
  case class InGameUsers(actorRef: ActorRef)
  case class ChatMessage(sender: Username, content: String, actorRef: ActorRef)
  case class ProcessGameMessage(username: Username, m : GameMessage)

  case class NewUser(username: Username, actor: ActorRef)
  case class UserLeft(username: Username, actor: ActorRef)
}