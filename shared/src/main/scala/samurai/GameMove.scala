package samurai

import samurai.Figure.Figure
import utils.MatrixPosition

sealed trait GameMove {
  def player: Int
}

case class AddToken(player: Int, tokenIndex: Int, location: MatrixPosition) extends GameMove
case class AddFigure(player: Int, figure: Figure, location: MatrixPosition) extends GameMove
case class EndTurn(player: Int) extends GameMove