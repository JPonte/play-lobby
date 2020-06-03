package controllers

import actions.{UserAction, UserRequest}
import actors.{LobbyActor, LobbyManager}
import akka.actor.{ActorSystem, Props}
import javax.inject.{Inject, Singleton}
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, WebSocket}

import scala.concurrent.{ExecutionContext, Future}
import akka.pattern.ask
import akka.util.Timeout
import core.{GameInfo, Username}
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

  private val lobbyManager = system.actorOf(Props[LobbyManager], "LobbyManager")
  private val gameRepository = new GameInfoRepository(db)

  def index(): Action[AnyContent] = userAction.async { implicit request: UserRequest[AnyContent] =>
    implicit val timeout: Timeout = Timeout(5.seconds)
    (lobbyManager ? LobbyManager.OnlineUserList()).map(_.asInstanceOf[Set[Username]]).map { currentUsers  =>
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
          LobbyActor.props(username, out, lobbyManager)
        })
    })
  }

  def samurai(): Action[AnyContent] = userAction { implicit request: UserRequest[AnyContent] =>
    request.username.fold(Redirect(routes.LoginController.login())) { _ =>
      val webSocketUrl = routes.LobbyController.socket().webSocketURL()
      Ok(views.html.samurai(webSocketUrl))
    }
  }

  def partyLobby(): Action[AnyContent] = userAction.async { implicit request: UserRequest[AnyContent] =>
    request.username.fold(Future.successful(Redirect(routes.LoginController.login()))) { username =>
      gameRepository.getWaitingGamesForUser(username).map(_.headOption).map {
        case Some(GameInfo(gameId, _, _, players, _)) =>
          val webSocketUrl = routes.LobbyController.socket().webSocketURL()
          Ok(views.html.party_lobby(webSocketUrl, username.value, gameId, players.map(_.value)))
        case _ =>
          Redirect(routes.LobbyController.index())
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

      gameRepository.createGame(2, pw).flatMap {
        case Some(gameInfo) =>
          gameRepository.joinGame(username, gameInfo.gameId).map {
            case true => Redirect(routes.LobbyController.partyLobby())
            case _ => Redirect(routes.LobbyController.index()).flashing("error" -> "Failed to join the game")
          }
        case _ =>
          Future.successful(Redirect(routes.LobbyController.index()).flashing("error" -> "Failed to create a game"))
      }
    }
  }

}
