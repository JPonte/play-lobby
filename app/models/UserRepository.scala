package models

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

//TODO use a db

@Singleton
class UserRepository @Inject()(implicit val executionContext: ExecutionContext) {

  private var users = Map("admin" -> "123", "ponte" -> "abc")

  def getUser(username: String): Future[Option[User]] = {
      Future{
        users.get(username).map(User(username, _))
      }
  }

  def addUser(user: User): Future[Boolean] = {
    getUser(user.username).map {
      case Some(_) => false
      case None if user.password.isEmpty => false
      case None =>
        users = users + (user.username -> user.password)
        true
    }
  }

}
