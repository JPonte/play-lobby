package controllers

import actions.{UserAction, UserRequest}
import actors.{ChatActor, ChatManager}
import akka.actor.{ActorSystem, Props}
import javax.inject.{Inject, Singleton}
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, WebSocket}

@Singleton
class LobbyController @Inject()(val controllerComponents: ControllerComponents,
                                userAction: UserAction,
                               )(implicit system: ActorSystem) extends BaseController {

  private val chatManager = system.actorOf(Props[ChatManager], "ChatManager")

  def index(): Action[AnyContent] = userAction { implicit request: UserRequest[AnyContent] =>
    request.username.fold(Redirect(routes.LoginController.login())) { _ =>
      val webSocketUrl = routes.LobbyController.socket().webSocketURL()
      Ok(views.html.index(webSocketUrl))
    }
  }

  def socket(): WebSocket = WebSocket.accept[String, String] { request =>
    val username = UserAction.extractUsername(request)
    println(s"$username connected")
    ActorFlow.actorRef { out =>
      username.fold(???){u => ChatActor.props(u, out, chatManager)}
    }
  }

}
