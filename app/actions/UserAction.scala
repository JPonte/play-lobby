package actions

import javax.inject._
import models.Username
import play.api.mvc.{ActionBuilder, AnyContent, BodyParser, PlayBodyParsers, Request, RequestHeader, Result, WrappedRequest}
import UserAction._

import scala.concurrent.{ExecutionContext, Future}

case class UserRequest[A](request: Request[A], username: Option[Username]) extends WrappedRequest[A](request)

@Singleton
class UserAction @Inject()(playBodyParsers: PlayBodyParsers)(implicit val executionContext: ExecutionContext) extends ActionBuilder[UserRequest, AnyContent] {
  override def parser: BodyParser[AnyContent] = playBodyParsers.defaultBodyParser

  override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
    val username = extractUsername(request)
    block(UserRequest(request, username))
  }
}

object UserAction {
  val USER_SESSION_COOKIE_ID = "user"

  def extractUsername(request: RequestHeader): Option[Username] =
    request.session.get(USER_SESSION_COOKIE_ID).map(Username)
}