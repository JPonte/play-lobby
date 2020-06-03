package samurai

import samurai.Board.Board
import samurai.Figure.Figure
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
  def placeFigure(figIndex: Int, boardPosition: MatrixPosition): Option[GameState] = {
    val playerState = players(nextPlayer)
    val figure = playerState.tokens(figIndex)
    val boardTile = board.get(boardPosition)
    ???
  }
}
