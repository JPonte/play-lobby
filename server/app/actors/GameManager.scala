package actors

import actors.GameManager._
import akka.actor.{Actor, ActorLogging, ActorRef}
import core.{GameInfo, GameSettings, PublicGameInfo, Username}
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Random

class GameManager extends Actor with ActorLogging {

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
      val updatedActors = usersMap.get(username).map(_ - actor).getOrElse(Set.empty[ActorRef])
      if (updatedActors.isEmpty) {
        usersMap = usersMap - username
      } else {
        usersMap = usersMap + (username -> updatedActors)
      }
      gameActors.get(gameId).foreach { gameRef =>
        gameRef ! GameActor.UserDisconnected(username, actor)
      }
    case GetGameInfo(gameId) =>
      gameActors.get(gameId).fold {
        sender() ! Option.empty[GameInfo]
      } { actor =>
        (actor ? GameActor.GetGameInfo).map(Option(_)).pipeTo(sender())
      }
    case GetGamesForUser(username) =>
      findGamesForUser(username).pipeTo(sender())
    case RequestJoinGame(username, gameId) =>
      gameActors.get(gameId) match {
        case Some(gameRef) => (gameRef ? GameActor.JoinGame(username)).mapTo[Int].map(Option(_)).pipeTo(sender())
        case _ => sender() ! Option.empty[Int]
      }
    case RequestLeaveGame(username, gameId) =>
      gameActors.get(gameId) match {
        case Some(gameRef) => (gameRef ? GameActor.LeaveGame(username)).mapTo[Boolean].pipeTo(sender())
        case _ => sender() ! false
      }
    case GetGames =>
      getAllGamesInfo.pipeTo(sender())
    case RequestStartGame(gameId) =>
      gameActors.get(gameId) match {
        case Some(actor) => (actor ? GameActor.StartGame).pipeTo(sender())
        case _ => sender() ! false
      }
    case m => log.error(s"Unhandled message $m")
  }

  private def getAllGamesInfo: Future[Seq[GameInfo]] = {
    Future.sequence(gameActors.values.toSeq.map(ga => (ga ? GameActor.GetGameInfo).mapTo[GameInfo]))
  }

  private def getAllGamesPublicInfo: Future[Seq[PublicGameInfo]] = getAllGamesInfo.map(_.map(PublicGameInfo(_)))

  private def findGamesForUser(username: Username): Future[Seq[GameInfo]] = {
    getAllGamesInfo.map(_.filter(_.players.contains(username)))
  }
}

object GameManager {

  case class CreateGame(settings: GameSettings, creator: Username)

  case class GameFinished(gameId: Int)

  case class GameUserConnected(username: Username, gameId: Int, actor: ActorRef)

  case class GameUserDisconnected(username: Username, gameId: Int, actor: ActorRef)

  case class RequestJoinGame(username: Username, gameId: Int)

  case class RequestLeaveGame(username: Username, gameId: Int)

  case class RequestStartGame(gameId: Int)

  case class GetGameInfo(gameId: Int)

  case object GetGames

  case class GetGamesForUser(username: Username)
}