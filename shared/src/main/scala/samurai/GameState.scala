package samurai

import samurai.Board.Board
import samurai.GameState._

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

case class GameState(board: Board, players: Map[Int, PlayerState], figuresDeck: FigureDeck, currentPlayer: Int, fullTokensPlayed: Int) {

  def getGamePhase: GamePhase = {
    if (figuresDeck.nonEmpty) FigurePlacementPhase
    else if (board.map(_._2.figures.size).sum == 0) {
      Finished
    } else
      TokenPlacementPhase
  }

  def play(gameMove: GameMove): Option[GameState] = {
    gameMove match {
      case AddFigure(`currentPlayer`, figure, location) if getGamePhase == FigurePlacementPhase && figuresDeck.figureCount(figure) > 0 =>
        board.get(location) match {
          case Some(bt @ BoardTile(Tile.City, figures, _)) if figures.size < 2 && !figures.contains(figure) =>
            Some(copy(board = board + (location -> bt.copy(figures = figures + figure))))
          case Some(bt @ BoardTile(Tile.Village, figures, _)) if figures.isEmpty =>
            Some(copy(board = board + (location -> bt.copy(figures = figures + figure))))
          case _ => None
        }
      case AddToken(`currentPlayer`, token, location) if getGamePhase == TokenPlacementPhase =>
        val playerState = players(currentPlayer) //TODO: update player state
        val fullTokensAdd = if(token.isInstanceOf[CharacterToken]) 0 else 1
        board.get(location) match {
          case Some(bt @ BoardTile(Tile.Land, _, None)) if !token.isInstanceOf[Ship] =>
            Some(copy(board = board + (location -> bt.copy(token = Some(token))), fullTokensPlayed = fullTokensPlayed + fullTokensAdd))
          case Some(bt @ BoardTile(Tile.Sea, _, None)) if token.isInstanceOf[Ship] =>
            Some(copy(board = board + (location -> bt.copy(token = Some(token))), fullTokensPlayed = fullTokensPlayed + fullTokensAdd))
          case _ => None
        }

      case EndTurn(`currentPlayer`) if fullTokensPlayed > 0=>
        Some(copy(currentPlayer = (currentPlayer + 1) % 2, fullTokensPlayed = 0))
      case _ =>
        None
    }
  }
}

object GameState {
  type GamePhase = Int
  val FigurePlacementPhase = 0
  val TokenPlacementPhase = 1
  val Finished = 2
}
