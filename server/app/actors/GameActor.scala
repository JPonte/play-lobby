package actors

import actors.GameActor.{ChatMessage, InGameUsers}
import actors.LobbyManager.PartyChatMessage
import akka.actor.{Actor, ActorRef}
import core.Username
import models.GameInfoRepository
import websocket.{ServerPartyChatMessage, ServerUpdatedPartyUsers}

import scala.concurrent.ExecutionContextExecutor

class GameActor(gameId: Int, gameInfoRepository: GameInfoRepository) extends Actor {

  println(s"GameActor for game $gameId started")

  override def receive: Receive = {
    case ChatMessage(sender, content, actorRef) =>
      implicit val ec: ExecutionContextExecutor = context.dispatcher
      gameInfoRepository.getGameInfo(gameId).map(_.foreach(gameInfo => actorRef ! PartyChatMessage(gameId, sender, content, gameInfo.players)))
    case InGameUsers(actorRef) =>
      implicit val ec: ExecutionContextExecutor = context.dispatcher
      gameInfoRepository.getGameInfo(gameId).map(_.foreach(gameInfo => actorRef ! ServerUpdatedPartyUsers(gameId, gameInfo.players.map(_.value))))
    case _ =>
  }
}

object GameActor {
  case class InGameUsers(actorRef: ActorRef)
  case class ChatMessage(sender: Username, content: String, actorRef: ActorRef)
}