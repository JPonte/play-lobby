package actors

import actors.GameManager._
import akka.actor.{Actor, ActorLogging, ActorRef}
import core.{GameInfo, GameSettings, Username}
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

class GameManager() extends Actor with ActorLogging {

  private implicit val defaultTimeout: Timeout = Timeout(5.seconds)
  implicit val ec: ExecutionContext = context.dispatcher

  private var usersMap = Map.empty[Username, Set[ActorRef]]
  private var gameActors = Map.empty[Int, ActorRef]

  override def receive: Receive = {
    case CreateGame(settings, creator) =>
      val gameId = Random.nextInt()
      val gameActor = context.actorOf(GameActor.props(gameId, settings), s"game-$gameId")
      gameActors = gameActors + (gameId -> gameActor)
      (gameActor ? GameActor.JoinGame(creator)).pipeTo(sender())
    case GameFinished(gameId) =>
      gameActors = gameActors - gameId
    case GameUserConnected(username, gameId, actor) =>
      val existingActors = usersMap.getOrElse(username, Set())
      usersMap = usersMap + (username -> (existingActors + actor))
      gameActors.get(gameId).foreach { gameRef =>
        actor ! GameWebSocketActor.ConnectToGame(gameId, gameRef)
        gameRef ! GameActor.UserConnected(username, actor)
      }
    case GameUserDisconnected(username, gameId, actor) =>
      //TODO: disconnect
      val updatedActors = usersMap.get(username).map(_ - actor).getOrElse(Set.empty[ActorRef])
      if (updatedActors.isEmpty) {
        usersMap = usersMap - username
      } else {
        usersMap = usersMap + (username -> updatedActors)
      }
    case GetGameInfo(gameId) =>
      gameActors.get(gameId).fold {
        sender() ! Option.empty[GameInfo]
      } { actor =>
        (actor ? GameActor.GetGameInfo).map(Option(_)).pipeTo(sender())
      }
    case m => log.error(s"Unhandled message $m")
  }
}

object GameManager {

  case class CreateGame(settings: GameSettings, creator: Username)

  case class GameFinished(gameId: Int)

  case class GameUserConnected(username: Username, gameId: Int, actor: ActorRef)

  case class GameUserDisconnected(username: Username, gameId: Int, actor: ActorRef)

  case class UserJoinedGame(username: Username, gameId: Int)

  case class UserLeftGame(username: Username, gameId: Int)

  case class GetGameInfo(gameId: Int)

}