package core

case class GameInfo(gameId: Int, playerCount: Int, password: String, players: Seq[Username], started: Boolean, finished: Boolean)
