package controllers

import actions.{UserAction, UserRequest}
import actors.{LobbyActor, LobbyManager}
import akka.actor.{ActorSystem, Props}
import javax.inject.{Inject, Singleton}
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, WebSocket}
import scala.concurrent.Future

@Singleton
class LobbyController @Inject()(val controllerComponents: ControllerComponents,
                                userAction: UserAction,
                               )(implicit system: ActorSystem) extends BaseController {

  private val chatManager = system.actorOf(Props[LobbyManager], "ChatManager")

  def index(): Action[AnyContent] = userAction { implicit request: UserRequest[AnyContent] =>
    request.username.fold(Redirect(routes.LoginController.login())) { _ =>
      val webSocketUrl = routes.LobbyController.socket().webSocketURL()
      Ok(views.html.index(webSocketUrl))
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
