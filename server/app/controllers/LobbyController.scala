package controllers

import actions.{UserAction, UserRequest}
import actors.LobbyManager.GameListChanged
import actors.{GameManager, GameWebSocketActor, LobbyManager, WebSocketActor}
import akka.actor.{ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import core.{GameInfo, GameSettings, PublicGameInfo, Username}
import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import slick.jdbc.JdbcProfile

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LobbyController @Inject()(val controllerComponents: ControllerComponents,
                                userAction: UserAction,
                                val dbConfigProvider: DatabaseConfigProvider
                               )(implicit system: ActorSystem, executionContext: ExecutionContext)
  extends BaseController with HasDatabaseConfigProvider[JdbcProfile] {

  private implicit val defaultTimeout: Timeout = Timeout(10.seconds)
  private val gameManager = system.actorOf(Props(classOf[GameManager]), "GameManager")
  private val lobbyManager = system.actorOf(Props(classOf[LobbyManager], gameManager), "LobbyManager")

  def index(): Action[AnyContent] = userAction.async { implicit request: UserRequest[AnyContent] =>
    request.username.fold(Future.successful(Redirect(routes.LoginController.login()))) { _ =>

      for {
        currentUsers <- (lobbyManager ? LobbyManager.RequestOnlineUserList()).map(_.asInstanceOf[Set[Username]])
      } yield {
        val webSocketUrl = routes.LobbyController.socket().webSocketURL()
        val username = request.username.map(_.value).getOrElse("")
        val usersSeq = currentUsers.map(_.value).toSeq
        Ok(views.html.index(webSocketUrl, username, usersSeq))
      }
    }
  }

  def socket(): WebSocket = WebSocket.acceptOrResult[String, String] { implicit request =>
    Future.successful(UserAction.extractUsername(request) match {
      case None => Left(Forbidden)
      case Some(username) =>
        Right(ActorFlow.actorRef { out =>
          WebSocketActor.props(username, out, lobbyManager)
        })
    })
  }

  def gameSocket(gameId: Int): WebSocket = WebSocket.acceptOrResult[String, String] { implicit request =>
    Future.successful(UserAction.extractUsername(request) match {
      case None => Left(Forbidden)
      case Some(username) =>
        Right(ActorFlow.actorRef { out =>
          GameWebSocketActor.props(username, gameId, out, gameManager)
        })
    })
  }

  def samurai(): Action[AnyContent] = userAction { implicit request: UserRequest[AnyContent] =>
    request.username.fold(Redirect(routes.LoginController.login())) { _ =>
      val webSocketUrl = routes.LobbyController.gameSocket(0).webSocketURL()
      Ok(views.html.samurai(webSocketUrl))
    }
  }

  def partyLobby(gameId: Int): Action[AnyContent] = userAction.async { implicit request: UserRequest[AnyContent] =>
    request.username.fold(Future.successful(Redirect(routes.LoginController.login()))) { username =>
      (gameManager ? GameManager.GetGameInfo(gameId)).map {
        case Some(GameInfo(gameId, _, _, players, _)) if players.contains(username) =>
          val webSocketUrl = routes.LobbyController.gameSocket(gameId).webSocketURL()
          Ok(views.html.party_lobby(webSocketUrl, username.value, gameId, players.map(_.value)))
        case _ =>
          Redirect(routes.LobbyController.index()).flashing("error" -> "Couldn't join game")
      }
    }
  }

  def createGame(): Action[AnyContent] = userAction.async { implicit request: UserRequest[AnyContent] =>
    request.username.fold(Future.successful(Redirect(routes.LoginController.login()))) { username =>
      val pw = (for {
        body <- request.body.asFormUrlEncoded
        password <- body.get("password").flatMap(_.headOption)
      } yield password.trim)
        .filter(_.nonEmpty)

      (gameManager ? GameManager.CreateGame(GameSettings(pw), username)).flatMap {
        case gameId: Int =>
          (gameManager ? GameManager.GetGames).mapTo[Seq[GameInfo]].map(games => GameListChanged(games.map(PublicGameInfo(_)))).pipeTo(lobbyManager)
          Future.successful(Redirect(routes.LobbyController.partyLobby(gameId)))
        case _ =>
          Future.successful(Redirect(routes.LobbyController.index()).flashing("error" -> "Failed to create a game"))
      }
    }
  }

  def joinGame(gameId: Int, password: Option[String]): Action[AnyContent] = userAction.async { implicit request: UserRequest[AnyContent] =>
    request.username.fold(Future.successful(Redirect(routes.LoginController.login()))) { username =>
      (gameManager ? GameManager.GetGameInfo(gameId)).mapTo[Option[GameInfo]].flatMap { gameInfo =>
        if (gameInfo.exists(_.password == password)) {
          (gameManager ? GameManager.RequestJoinGame(username, gameId)).mapTo[Option[Int]].map {
            case Some(_) =>
              Redirect(routes.LobbyController.partyLobby(gameId))
            case _ =>
              Redirect(routes.LobbyController.index()).flashing("error" -> "Failed to join the game")
          }
        } else {
          Future.successful(Redirect(routes.LobbyController.index()).flashing("error" -> "Wrong password"))
        }
      }
    }
  }

}
