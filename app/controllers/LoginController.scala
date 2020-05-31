package controllers

import actions._
import javax.inject._
import models.UserRepository
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LoginController @Inject()(val controllerComponents: ControllerComponents,
                                userAction: UserAction,
                                userRepository: UserRepository)(implicit executionContext: ExecutionContext) extends BaseController {

  def login(): Action[AnyContent] = userAction { implicit request: UserRequest[AnyContent] =>
    request.username.fold(Ok(views.html.login())) { _ =>
      Redirect(routes.LobbyController.index())
    }
  }

  def logout(): Action[AnyContent] = userAction { implicit request: UserRequest[AnyContent] =>
    Redirect(routes.LoginController.login()).withSession(Session.emptyCookie)
  }

  def validateLogin(): Action[AnyContent] = Action.async { implicit request =>
    (for {
      body <- request.body.asFormUrlEncoded
      username <- body.get("username").flatMap(_.headOption)
      password <- body.get("password").flatMap(_.headOption)
    } yield {
      userRepository.getUser(username).map { user =>
        if (user.exists(_.password == password)) {
          Redirect(routes.LobbyController.index()).withSession(UserAction.USER_SESSION_COOKIE_ID -> username)
        } else {
          Redirect(routes.LoginController.login()).flashing("error" -> "Wrong username/password")
        }
      }
    }).getOrElse(Future {
      Redirect(routes.LoginController.login()).flashing("error" -> "Invalid login")
    })
  }
}
