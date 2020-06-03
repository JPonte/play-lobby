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
import core.Username

import scala.concurrent.duration._

@Singleton
class LobbyController @Inject()(val controllerComponents: ControllerComponents,
                                userAction: UserAction
                               )(implicit system: ActorSystem, executionContext: ExecutionContext) extends BaseController {

  private val lobbyManager = system.actorOf(Props[LobbyManager], "LobbyManager")

  def index(): Action[AnyContent] = userAction.async { implicit request: UserRequest[AnyContent] =>
    implicit val timeout: Timeout = Timeout(5.seconds)
    (lobbyManager ? LobbyManager.UserList()).map(_.asInstanceOf[Set[Username]]).map { currentUsers  =>
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

  def partyLobby(): Action[AnyContent] = userAction { implicit request: UserRequest[AnyContent] =>
    request.username.fold(Redirect(routes.LoginController.login())) { username =>
      val webSocketUrl = routes.LobbyController.socket().webSocketURL()
      Ok(views.html.party_lobby(webSocketUrl, username.value, Seq(username.value, "ponte", "merdas")))
    }
  }

}
