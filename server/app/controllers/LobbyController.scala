package controllers

import actions.{UserAction, UserRequest}
import actors.{GameActor, GameManager, GameWebSocketActor, LobbyManager, WebSocketActor}
import akka.actor.{ActorRef, ActorSystem, Props}
import javax.inject.{Inject, Singleton}
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, WebSocket}

import scala.concurrent.{ExecutionContext, Future}
import akka.pattern.ask
import akka.util.Timeout
import core.{GameInfo, GameSettings, Username}
import models.GameInfoRepository
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.duration._

@Singleton
class LobbyController @Inject()(val controllerComponents: ControllerComponents,
                                userAction: UserAction,
                                val dbConfigProvider: DatabaseConfigProvider
                               )(implicit system: ActorSystem, executionContext: ExecutionContext)
  extends BaseController with HasDatabaseConfigProvider[JdbcProfile] {

  private implicit val defaultTimeout: Timeout = Timeout(10.seconds)
  private val lobbyManager = system.actorOf(Props[LobbyManager], "LobbyManager")
  private val gameManager = system.actorOf(Props[GameManager], "GameManager")
  //  private val gameRepository = new GameInfoRepository(db)
  //  gameRepository.getAllWaitingGames.foreach(_.foreach { gameInfo =>
  //    startGameActor(gameInfo.gameId)
  //  })

  def index(): Action[AnyContent] = userAction.async { implicit request: UserRequest[AnyContent] =>
    (lobbyManager ? LobbyManager.RequestOnlineUserList()).map(_.asInstanceOf[Set[Username]]).map { currentUsers =>
      request.username.fold(Redirect(routes.LoginController.login())) { _ =>
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
          Future.successful(Redirect(routes.LobbyController.partyLobby(gameId)))
        case _ =>
          Future.successful(Redirect(routes.LobbyController.index()).flashing("error" -> "Failed to create a game"))
      }
    }
  }

}
