package samurai

import samurai.Board.Board
import samurai.Figure.Figure
import samurai.GameState._
import utils.MatrixPosition

import scala.annotation.tailrec

case class PlayerState(playerId: Int, tokens: Seq[Token], deck: Seq[Token]) {
  def drawTokens(count: Int): PlayerState = {
    @tailrec
    def drawTokensAux(playerState: PlayerState, count: Int): PlayerState = {
      if (count < 0)
        playerState
      else
        drawTokensAux(playerState.copy(tokens = playerState.tokens ++ Seq(playerState.deck.head), deck = playerState.deck.tail), count - 1)
    }

    assert(count >= deck.size)
    drawTokensAux(this, Math.min(count, deck.size))
  }
}

case class GameState(board: Board, players: Map[Int, PlayerState], figuresDeck: Seq[Figure], nextPlayer: Int) {

  def getGamePhase: GamePhase = {
    if (figuresDeck.nonEmpty) FigurePlacementPhase
    else if (board.map(_._2.figures.size).sum == 0) {
      Finished
    } else
      TokenPlacementPhase
  }

  def play(gameMove: GameMove): Option[GameState] = {
    gameMove match {
      case AddFigure(`nextPlayer`, figure, location) if getGamePhase == FigurePlacementPhase =>
        val playerState = players(nextPlayer)
        val figure = figuresDeck.head
        val boardTile = board.get(location)
      case AddToken(`nextPlayer`, token, location) if getGamePhase == TokenPlacementPhase =>
      case _ =>
        // Invalid play
    }
    ???
  }
}

object GameState {
  type GamePhase = Int
  val FigurePlacementPhase = 0
  val TokenPlacementPhase = 1
  val Finished = 2
}
