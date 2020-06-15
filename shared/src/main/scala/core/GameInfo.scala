package core

import core.GameStatus.GameStatus

object GameStatus {
  type GameStatus = Int
  val WaitingToStart = 0
  val Running = 1
  val Finished = 2
}

case class GameSettings(name: String, password: Option[String])
case class GameInfo(gameId: Int, name: String, playerCount: Int, password: Option[String], players: Seq[Username], status: GameStatus)

case class PublicGameInfo(gameId: Int, name: String, maxPlayerCount: Int, hasPassword: Boolean, playerCount: Int, status: GameStatus, isUserIn: Boolean)

object PublicGameInfo {
  def apply(gameInfo: GameInfo, targetUser: Username): PublicGameInfo =
    new PublicGameInfo(gameInfo.gameId, gameInfo.name, gameInfo.playerCount, gameInfo.password.nonEmpty, gameInfo.players.size, gameInfo.status, gameInfo.players.contains(targetUser))
}