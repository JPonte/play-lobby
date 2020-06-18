package controllers

import actions._
import core.User
import javax.inject._
import models.DatabaseUserRepository
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LoginController @Inject()(val controllerComponents: ControllerComponents,
                                userAction: UserAction,
                                val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends BaseController with HasDatabaseConfigProvider[JdbcProfile] {

  val userRepository = new DatabaseUserRepository(db)

  def login(): Action[AnyContent] = userAction { implicit request: UserRequest[AnyContent] =>
    request.username.fold(Ok(views.html.login())) { _ =>
      Redirect(routes.LobbyController.index())
    }
  }

  def register(): Action[AnyContent] = userAction { implicit request: UserRequest[AnyContent] =>
    request.username.fold(Ok(views.html.register())) { _ =>
      Redirect(routes.LobbyController.index())
    }
  }

  def logout(): Action[AnyContent] = userAction { implicit request: UserRequest[AnyContent] =>
    Redirect(routes.LoginController.login()).withSession(Session.emptyCookie)
  }

  def validateLogin(): Action[AnyContent] = Action.async { implicit request =>
    (for {
      body <- request.body.asFormUrlEncoded
      username <- body.get("username").flatMap(_.headOption).map(_.trim)
      password <- body.get("password").flatMap(_.headOption).map(_.trim)
    } yield {
      userRepository.validateUser(username, password).map {
        case true => Redirect(routes.LobbyController.index()).withSession(UserAction.USER_SESSION_COOKIE_ID -> username)
        case false => Redirect(routes.LoginController.login()).flashing("error" -> "Wrong username/password")
      }
    }).getOrElse(Future {
      Redirect(routes.LoginController.login()).flashing("error" -> "Invalid login")
    })
  }

  def validateRegister(): Action[AnyContent] = Action.async { implicit request =>
    val newUser = for {
      body <- request.body.asFormUrlEncoded
      username <- body.get("username").flatMap(_.headOption)
      if username.trim.nonEmpty
      password <- body.get("password").flatMap(_.headOption)
      if password.trim.nonEmpty
    } yield User(username.trim, password.trim)

    newUser.map(userRepository.addUser).getOrElse(Future.successful(false)).map {
      case true => Redirect(routes.LobbyController.index()).withSession(UserAction.USER_SESSION_COOKIE_ID -> newUser.map(_.username).get)
      case false => Redirect(routes.LoginController.register()).flashing("error" -> s"The username ${newUser.map(_.username).get} already exists")
    }
  }
}
