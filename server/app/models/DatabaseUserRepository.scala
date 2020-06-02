package models

import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}
import models.Tables._
import org.mindrot.jbcrypt.BCrypt
import scala.util.Try

class DatabaseUserRepository(db: Database)(implicit val executionContext: ExecutionContext) {

  def validateUser(username: String, password: String): Future[Boolean] = {
    getUser(username).map{
      case Some(user) =>
        Try(BCrypt.checkpw(password, user.password)).getOrElse(false)
      case _ => false
    }
  }

  def getUser(username: String): Future[Option[User]] = {
    val matches = db.run(Users.filter(userRow => userRow.username === username).result)
    matches.map(_.map(userRow => User(userRow.username, userRow.password)).headOption)
  }

  def addUser(user: User): Future[Boolean] = {
    getUser(user.username).flatMap {
      case Some(_) => Future.successful(false)
      case None =>
        val hashedPw = BCrypt.hashpw(user.password, BCrypt.gensalt())
        db.run(Users.insertOrUpdate(UsersRow(user.username, hashedPw))).map(_ > 0)
    }
  }

}
