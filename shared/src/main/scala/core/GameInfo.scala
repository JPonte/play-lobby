package core

import core.GameStatus.GameStatus

object GameStatus {
  type GameStatus = Int
  val WaitingToStart = 0
  val Running = 1
  val Finished = 2
}

case class GameInfo(gameId: Int, playerCount: Int, password: Option[String], players: Seq[Username], status: GameStatus)

case class PublicGameInfo(gameId: Int, maxPlayerCount: Int, hasPassword: Boolean, playerCount: Int, status: GameStatus)

object PublicGameInfo {
  def apply(gameInfo: GameInfo): PublicGameInfo =
    new PublicGameInfo(gameInfo.gameId, gameInfo.playerCount, gameInfo.password.nonEmpty, gameInfo.playerCount, gameInfo.status)
}