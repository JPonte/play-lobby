package models

import core.{GameInfo, GameStatus, Username}
import models.Tables.{GameInfoRow, GamePlayersRow}
import slick.jdbc.PostgresProfile.api._

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class GameInfoRepository(db: Database)(implicit val executionContext: ExecutionContext) {

  private def dbToGameInfo(info: Tables.GameInfoRow, players: Seq[GamePlayersRow]) = {
    GameInfo(info.id, "", info.playerCount, info.password, players.map(p => Username(p.username)), info.status)
  }

  def getGamesForUser(username: Username): Future[immutable.Iterable[GameInfo]] = {
    val dbQuery = (for {
      gamePlayer <- Tables.GamePlayers.filter(_.username === username.value)
      game <- Tables.GameInfo if game.id === gamePlayer.gameId
      gamePlayers <- Tables.GamePlayers.filter(_.gameId === game.id)
    } yield (game, gamePlayers)).result
    db.run(dbQuery).map(_.groupBy(_._1).map(r => dbToGameInfo(r._1, r._2.map(_._2))))
  }

  def getRunningGamesForUser(username: Username): Future[immutable.Iterable[GameInfo]] =
    getGamesForUser(username).map(_.filter(_.status == GameStatus.Running))

  def getWaitingGamesForUser(username: Username): Future[immutable.Iterable[GameInfo]] =
    getGamesForUser(username).map(_.filter(_.status == GameStatus.WaitingToStart))

  def getFinishedGamesForUser(username: Username): Future[immutable.Iterable[GameInfo]] =
    getGamesForUser(username).map(_.filter(_.status == GameStatus.Finished))

  def getGameInfo(gameId: Int): Future[Option[GameInfo]] = {
    val dbQuery = (for {
      game <- Tables.GameInfo.filter(_.id === gameId) joinLeft Tables.GamePlayers on (_.id === _.gameId)
    } yield (game._1, game._2)).result
    db.run(dbQuery).map(_.groupBy(_._1).headOption.map(r => dbToGameInfo(r._1, r._2.flatMap(_._2))))
  }

  def getAllRunningGames: Future[immutable.Iterable[GameInfo]] = {
    val dbQuery = (for {
      game <- Tables.GameInfo.filter(_.status === GameStatus.Running) joinLeft Tables.GamePlayers on (_.id === _.gameId)
    } yield (game._1, game._2)).result
    db.run(dbQuery).map(_.groupBy(_._1).map(r => dbToGameInfo(r._1, r._2.flatMap(_._2))))
  }

  def getAllWaitingGames: Future[immutable.Iterable[GameInfo]] = {
    val dbQuery = (for {
      game <- Tables.GameInfo.filter(_.status === GameStatus.WaitingToStart) joinLeft Tables.GamePlayers on (_.id === _.gameId)
    } yield (game._1, game._2)).result
    db.run(dbQuery).map(_.groupBy(_._1).map(r => dbToGameInfo(r._1, r._2.flatMap(_._2))))
  }

  def joinGame(username: Username, gameId: Int): Future[Boolean] = {
    getGameInfo(gameId).map(_.exists { gameInfo =>
      gameInfo.status == GameStatus.WaitingToStart && gameInfo.playerCount > gameInfo.players.size && !gameInfo.players.contains(username)
    }).flatMap {
      case true =>
        db.run(Tables.GamePlayers += GamePlayersRow(gameId, username.value)).map(_ > 0)
      case _ =>
        Future.successful(false)
    }
  }

  def leaveGame(username: Username, gameId: Int): Future[Boolean] = {
    getGameInfo(gameId).map(_.exists { gameInfo =>
      gameInfo.status == GameStatus.WaitingToStart && gameInfo.players.contains(username)
    }).flatMap {
      case true =>
        db.run(Tables.GamePlayers.filter(gp => gp.username === username.value && gp.gameId === gameId).delete).map(_ > 0)
      case _ =>
        Future.successful(false)
    }
  }

  def createGame(playerCount: Int, password: Option[String]): Future[Option[GameInfo]] = {
    db.run(Tables.GameInfo.returning(Tables.GameInfo.map(_.id)) += GameInfoRow(-1, playerCount, password))
      .flatMap(getGameInfo)
  }

  def startGame(gameId: Int): Future[Boolean] = {
    db.run((for {gi <- Tables.GameInfo if gi.id === gameId && gi.status === GameStatus.WaitingToStart} yield gi.status)
      .update(GameStatus.Running)).map(_ > 0)
  }

  def finishGame(gameId: Int): Future[Boolean] = {
    db.run((for {gi <- Tables.GameInfo if gi.id === gameId && gi.status === GameStatus.Running} yield gi.status)
      .update(GameStatus.Finished)).map(_ > 0)
  }
}
