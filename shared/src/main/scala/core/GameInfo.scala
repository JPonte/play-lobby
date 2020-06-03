package core

import core.GameStatus.GameStatus

object GameStatus {
  type GameStatus = Int
  val WaitingToStart = 0
  val Running = 1
  val Finished = 2
}

case class GameInfo(gameId: Int, playerCount: Int, password: Option[String], players: Seq[Username], status: GameStatus)
