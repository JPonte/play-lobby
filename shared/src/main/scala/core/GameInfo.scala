package core

import core.GameStatus.GameStatus

object GameStatus {
  type GameStatus = Int
  val WaitingToStart = 0
  val Running = 1
  val Finished = 2
}

case class GameSettings(name: String, password: Option[String])
case class GameInfo(gameId: Int, name: String, maxPlayers: Int, minPlayers: Int, password: Option[String], players: Seq[Username], status: GameStatus) {
  def canStart: Boolean = status == GameStatus.WaitingToStart && players.size >= minPlayers && players.size <= maxPlayers
}

case class PublicGameInfo(gameId: Int, name: String, maxPlayerCount: Int, hasPassword: Boolean, playerCount: Int, status: GameStatus, isUserIn: Boolean)

object PublicGameInfo {
  def apply(gameInfo: GameInfo, targetUser: Username): PublicGameInfo =
    new PublicGameInfo(gameInfo.gameId, gameInfo.name, gameInfo.maxPlayers, gameInfo.password.nonEmpty, gameInfo.players.size, gameInfo.status, gameInfo.players.contains(targetUser))
}