package actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Stash}
import core.Username
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import websocket._
import GameWebSocketActor._

class GameWebSocketActor(username: Username, gameId: Int, out: ActorRef, gameManager: ActorRef) extends Actor with Stash with ActorLogging{

  gameManager ! GameManager.GameUserConnected(username, gameId, self)

  private var gameActor = Option.empty[ActorRef]

  override def receive: Receive = {
    case message: String =>
      log.info(s"$message")
      decode[ClientWebSocketMessage](message) match {
        case Right(m: GameMessage) =>
          gameActor match {
            case Some(gameActor) =>
              log.debug(s"Got $m")
              gameActor ! GameActor.ProcessGameMessage(username, m)
            case None =>
              log.debug(s"Stashing message since there is no GameActor ref yet")
              stash()
          }
        case _ =>
          log.error(s"Invalid message $message")
      }
    case SendToClient(message) =>
      log.debug(s"Sending $message")
      out ! message.asJson.spaces2
    case ConnectToGame(gameId, actorRef) =>
      if (gameId == this.gameId) {
        gameActor = Some(actorRef)
      unstashAll()
      } else {
        log.error(s"Wrong game to connect to: $gameId - ${this.gameId}")
      }
    case DisconnectFromGame(gameId) =>
      if (gameId == this.gameId) {
        self ! PoisonPill
      } else {
        log.error(s"Wrong game to disconnect from: $gameId - ${this.gameId}")
      }
    case m =>
      log.warning(s"Unhandled message: $m")
  }

  override def postStop(): Unit = {
    gameManager ! GameManager.GameUserDisconnected(username, gameId, self)
  }

}

object GameWebSocketActor {
  def props(username: Username, gameId: Int, out: ActorRef, gameManager: ActorRef): Props = Props(new GameWebSocketActor(username, gameId, out, gameManager))

  case class SendToClient(message: ServerWebSocketMessage)
  case class ConnectToGame(gameId: Int, gameActor: ActorRef)
  case class DisconnectFromGame(gameId: Int)
}