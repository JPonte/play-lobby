package samurai

import core.Username
import samurai.Board.Board
import samurai.Figure.Figure
import samurai.GameState._
import samurai.PlayerState._

import scala.annotation.tailrec
import scala.util.Random

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

case class GameState(board: Board, players: Map[PlayerId, PlayerState], figuresDeck: FigureDeck, currentPlayer: PlayerId, fullTokensPlayed: Boolean, characterTokensPlayed: Boolean) {

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
        val fullTokensAdd = !token.isInstanceOf[CharacterToken]
        val newPlayerState = playerState.copy(tokens = playerState.tokens.zipWithIndex.filter(_._2 != tokenIndex).map(_._1))
        board.get(location) match {
          case Some(bt@BoardTile(Tile.Land, _, None)) if !token.isInstanceOf[Ship] && !(fullTokensAdd && fullTokensPlayed) =>
            Some(copy(board = board + (location -> bt.copy(token = Some(token))), fullTokensPlayed = fullTokensPlayed || fullTokensAdd, characterTokensPlayed = characterTokensPlayed || !fullTokensAdd, players = players + (currentPlayer -> newPlayerState)).resolveCapture)
          case Some(bt@BoardTile(Tile.Sea, _, None)) if token.isInstanceOf[Ship] =>
            Some(copy(board = board + (location -> bt.copy(token = Some(token))), fullTokensPlayed = fullTokensPlayed || fullTokensAdd, characterTokensPlayed = characterTokensPlayed || !fullTokensAdd, players = players + (currentPlayer -> newPlayerState)).resolveCapture)
          case _ => None
        }

      case EndTurn(`currentPlayer`) if fullTokensPlayed || characterTokensPlayed =>
        val playerState = players(currentPlayer)
        val tokensToDraw = 5 - playerState.tokens.size
        val (newDeck, extraHand) = pickRandomN(playerState.deck, tokensToDraw)
        val newPlayerState = playerState.copy(tokens = playerState.tokens ++ extraHand, deck = newDeck)
        Some(copy(currentPlayer = (currentPlayer + 1) % 2, fullTokensPlayed = false, characterTokensPlayed = false, players = players + (newPlayerState.playerId -> newPlayerState)))
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

  def initialGameState(players: Seq[Username], addFiguresAuto: Boolean = false): GameState = {

    val playerStates = players.zipWithIndex.map { case (_, i) =>

      var tokens: Seq[Token] = Seq(
        FigureToken(Figure.RiceField, 2, i),
        FigureToken(Figure.RiceField, 3, i),
        FigureToken(Figure.RiceField, 4, i),
        FigureToken(Figure.Helmet, 2, i),
        FigureToken(Figure.Helmet, 3, i),
        FigureToken(Figure.Helmet, 4, i),
        FigureToken(Figure.Buddha, 2, i),
        FigureToken(Figure.Buddha, 3, i),
        FigureToken(Figure.Buddha, 4, i),
        SamuraiToken(1, i),
        SamuraiToken(1, i),
        SamuraiToken(2, i),
        SamuraiToken(2, i),
        SamuraiToken(3, i),
        Ronin(1, i),
        Ship(1, i),
        Ship(1, i),
        Ship(2, i),
      )

      val (newTokens, hand) = pickRandomN(tokens, 5)
      tokens = newTokens

      i -> PlayerState(i, hand, tokens, FigureDeck(0, 0, 0))
    }.toMap

    val board = Board.twoPlayerBoard.map {
      case (coords, t) =>
        val figures = if (t == Tile.Edo) {
          Set(Figure.Buddha, Figure.Helmet, Figure.RiceField)
        } else {
          Set.empty[Figure]
        }
        coords -> BoardTile(t, figures, None)
    }

    var gameState = GameState(board, playerStates, FigureDeck(6, 6, 6), 0, fullTokensPlayed = false, characterTokensPlayed = false)

    if (addFiguresAuto) {
      while (gameState.figuresDeck.nonEmpty) {
        val unsetCities = gameState.board.filter {
          case (_, BoardTile(Tile.City, figures, _)) => figures.size < 2
          case _ => false
        }
        val unsetVillages = gameState.board.filter {
          case (_, BoardTile(Tile.Village, figures, _)) => figures.isEmpty
          case _ => false
        }
        val unsetTiles = if (unsetCities.nonEmpty) unsetCities else unsetVillages
        val tileIndex = Random.nextInt(unsetTiles.size)
        val tile = unsetTiles.toSeq(tileIndex)

        val availableFigures = gameState.figuresDeck.availableFigures -- tile._2.figures
        val figureIndex = Random.nextInt(availableFigures.size)
        val figure = availableFigures.toSeq(figureIndex)

        val gameMove = AddFigure(gameState.currentPlayer, figure, tile._1)
        gameState.play(gameMove).foreach(gameState = _)
      }
    }
    gameState
  }

  @scala.annotation.tailrec
  def pickRandomN[T](seq: Seq[T], n: Int, acc: Seq[T] = Seq.empty[T]): (Seq[T], Seq[T]) = {
    if (seq.isEmpty || n == 0) {
      (seq, acc)
    } else {
      val (newSeq, newT) = pickRandom[T](seq)
      pickRandomN(newSeq, n - 1, acc ++ Seq(newT).flatten)
    }
  }

  def pickRandom[T](seq: Seq[T]): (Seq[T], Option[T]) = {
    if (seq.isEmpty) {
      (seq, None)
    } else {
      val random = Random.nextInt(seq.size)
      val t = seq(random)
      val resultSeq = seq.take(random) ++ seq.takeRight(seq.size - random - 1)

      (resultSeq, Some(t))
    }
  }
}
