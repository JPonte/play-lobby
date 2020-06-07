package samurai

import samurai.Board.Board
import samurai.Figure.Figure
import samurai.GameState._
import samurai.PlayerState._

import scala.annotation.tailrec

case class PlayerState(playerId: Int, tokens: Seq[Token], deck: Seq[Token], figureDeck: FigureDeck) {
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

object PlayerState {
  type PlayerId = Int
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
          case Some(bt@BoardTile(Tile.City, figures, _)) if figures.size < 2 && !figures.contains(figure) =>
            Some(copy(board = board + (location -> bt.copy(figures = figures + figure)), figuresDeck = figuresDeck.removeFigure(figure)).resolveCapture)
          case Some(bt@BoardTile(Tile.Village, figures, _)) if figures.isEmpty =>
            Some(copy(board = board + (location -> bt.copy(figures = figures + figure)), figuresDeck = figuresDeck.removeFigure(figure)).resolveCapture)
          case _ => None
        }
      case AddToken(`currentPlayer`, tokenIndex, location) if getGamePhase == TokenPlacementPhase && players(currentPlayer).tokens.size > tokenIndex =>
        val playerState = players(currentPlayer)
        val token = playerState.tokens(tokenIndex)
        val fullTokensAdd = if (token.isInstanceOf[CharacterToken]) 0 else 1
        val newPlayerState = playerState.copy(tokens = playerState.tokens.zipWithIndex.filter(_._2 != tokenIndex).map(_._1))
        board.get(location) match {
          case Some(bt@BoardTile(Tile.Land, _, None)) if !token.isInstanceOf[Ship] =>
            Some(copy(board = board + (location -> bt.copy(token = Some(token))), fullTokensPlayed = fullTokensPlayed + fullTokensAdd, players = players + (currentPlayer -> newPlayerState)).resolveCapture)
          case Some(bt@BoardTile(Tile.Sea, _, None)) if token.isInstanceOf[Ship] =>
            Some(copy(board = board + (location -> bt.copy(token = Some(token))), fullTokensPlayed = fullTokensPlayed + fullTokensAdd, players = players + (currentPlayer -> newPlayerState)).resolveCapture)
          case _ => None
        }

      case EndTurn(`currentPlayer`) if fullTokensPlayed > 0 =>
        Some(copy(currentPlayer = (currentPlayer + 1) % 2, fullTokensPlayed = 0))
      case _ =>
        None
    }
  }

  private def resolveCapture: GameState = {
    val newBoard = board.map {
      case x@(position, boardTile) if Tile.Settlements.contains(boardTile.tile) =>
        val neighbourTiles = Board.getNeighbours(position).flatMap(board.get)
        val landTiles = neighbourTiles.filter(_.tile == Tile.Land)
        if (landTiles.forall(_.token.nonEmpty)) {
          val seaTiles = neighbourTiles.filter(_.tile == Tile.Sea)

          val scores = (landTiles ++ seaTiles).toSeq.flatMap(_.token).collect {
            case SamuraiToken(influence, playerId) => Map(Figure.RiceField -> Map(playerId -> influence), Figure.Helmet -> Map(playerId -> influence), Figure.Buddha -> Map(playerId -> influence))
            case Ronin(influence, playerId) => Map(Figure.RiceField -> Map(playerId -> influence), Figure.Helmet -> Map(playerId -> influence), Figure.Buddha -> Map(playerId -> influence))
            case Ship(influence, playerId) => Map(Figure.RiceField -> Map(playerId -> influence), Figure.Helmet -> Map(playerId -> influence), Figure.Buddha -> Map(playerId -> influence))
            case FigureToken(figure, influence, playerId) => Map(figure -> Map(playerId -> influence))
          }.foldLeft(Map.empty[Figure, Map[PlayerId, Int]]) { case (map, scores) =>
            map ++ scores.map { case (fig, playerScore) =>
              fig -> (map.getOrElse(fig, Map()) ++ playerScore.map { case (player, score) =>
                player -> (score + map.get(fig).flatMap(_.get(player)).getOrElse(0))
              })
            }
          }

          val result = scores.collect {
            case (figure, scores) if boardTile.figures.contains(figure) && scores.size == 1 =>
              figure -> Option(scores.head._1)
            case (figure, scores) if boardTile.figures.contains(figure) =>
              val sortedScores = scores.toSeq.sortBy(-_._2)
              if (sortedScores.head._2 > sortedScores(1)._2)
                figure -> Option(sortedScores.head._1)
              else
                figure -> None
          }

          //TODO: Actually use the result...

          println(result)

          position -> boardTile.copy(figures = Set())
        } else {
          x
        }
      case x => x
    }

    copy(board = newBoard)
  }
}

object GameState {
  type GamePhase = Int
  val FigurePlacementPhase = 0
  val TokenPlacementPhase = 1
  val Finished = 2
}
