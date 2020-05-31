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
import models.Username

import scala.concurrent.duration._

@Singleton
class LobbyController @Inject()(val controllerComponents: ControllerComponents,
                                userAction: UserAction
                               )(implicit system: ActorSystem, executionContext: ExecutionContext) extends BaseController {

  private val chatManager = system.actorOf(Props[LobbyManager], "ChatManager")

  def index(): Action[AnyContent] = userAction.async { implicit request: UserRequest[AnyContent] =>
    implicit val timeout: Timeout = Timeout(5.seconds)
    (chatManager ? LobbyManager.UserList()).map(_.asInstanceOf[Set[Username]]).map { currentUsers  =>
      request.username.fold(Redirect(routes.LoginController.login())) { _ =>
        val webSocketUrl = routes.LobbyController.socket().webSocketURL()
        Ok(views.html.index(webSocketUrl, request.username.map(_.value).getOrElse(""), currentUsers.map(_.value).toSeq))
      }
    }
  }

  def socket(): WebSocket = WebSocket.acceptOrResult[String, String] { implicit request =>
    Future.successful(UserAction.extractUsername(request) match {
      case None => Left(Forbidden)
      case Some(username) =>
        Right(ActorFlow.actorRef { out =>
          LobbyActor.props(username, out, chatManager)
        })
    })
  }

}
