package samurai

import samurai.Figure.Figure
import samurai.Tile.Tile
import utils._

case class BoardTile(tile: Tile, figures: Set[Figure], token: Option[Token])

object Board {

  def getNeighbours(position: MatrixPosition): Set[MatrixPosition] = {
    if (position.row % 2 == 0) {
      Set(
        MatrixPosition(position.column - 1, position.row),
        MatrixPosition(position.column + 1, position.row),
        MatrixPosition(position.column, position.row + 1),
        MatrixPosition(position.column + 1, position.row + 1),
        MatrixPosition(position.column, position.row - 1),
        MatrixPosition(position.column + 1, position.row - 1)
      )
    } else {
      Set(
        MatrixPosition(position.column - 1, position.row),
        MatrixPosition(position.column + 1, position.row),
        MatrixPosition(position.column, position.row + 1),
        MatrixPosition(position.column - 1, position.row + 1),
        MatrixPosition(position.column, position.row - 1),
        MatrixPosition(position.column - 1, position.row - 1)
      )
    }
  }

  type BaseBoard = SparseMatrix[Tile]
  type Board = SparseMatrix[BoardTile]

  val twoPlayerBoard: BaseBoard = Map(
    MatrixPosition(5, 6) -> 3,
    MatrixPosition(9, 6) -> 2,
    MatrixPosition(11, 10) -> 1,
    MatrixPosition(13, 2) -> 2,
    MatrixPosition(4, 8) -> 1,
    MatrixPosition(12, 1) -> 1,
    MatrixPosition(8, 5) -> 2,
    MatrixPosition(10, 8) -> 2,
    MatrixPosition(10, 5) -> 2,
    MatrixPosition(11, 8) -> 2,
    MatrixPosition(14, 1) -> 1,
    MatrixPosition(9, 9) -> 1,
    MatrixPosition(10, 10) -> 1,
    MatrixPosition(7, 8) -> 2,
    MatrixPosition(11, 6) -> 2,
    MatrixPosition(7, 5) -> 1,
    MatrixPosition(4, 7) -> 3,
    MatrixPosition(7, 9) -> 1,
    MatrixPosition(2, 5) -> 1,
    MatrixPosition(6, 10) -> 1,
    MatrixPosition(14, 3) -> 3,
    MatrixPosition(2, 6) -> 3,
    MatrixPosition(8, 4) -> 1,
    MatrixPosition(13, 1) -> 3,
    MatrixPosition(10, 9) -> 1,
    MatrixPosition(9, 7) -> 2,
    MatrixPosition(1, 8) -> 1,
    MatrixPosition(14, 2) -> 1,
    MatrixPosition(6, 5) -> 1,
    MatrixPosition(4, 5) -> 1,
    MatrixPosition(4, 10) -> 1,
    MatrixPosition(5, 8) -> 2,
    MatrixPosition(13, 0) -> 1,
    MatrixPosition(4, 6) -> 2,
    MatrixPosition(12, 2) -> 2,
    MatrixPosition(5, 10) -> 1,
    MatrixPosition(12, 3) -> 4,
    MatrixPosition(12, 8) -> 1,
    MatrixPosition(11, 7) -> 2,
    MatrixPosition(6, 7) -> 2,
    MatrixPosition(4, 9) -> 1,
    MatrixPosition(2, 7) -> 2,
    MatrixPosition(10, 4) -> 1,
    MatrixPosition(9, 4) -> 1,
    MatrixPosition(12, 4) -> 2,
    MatrixPosition(13, 6) -> 1,
    MatrixPosition(12, 9) -> 1,
    MatrixPosition(1, 6) -> 2,
    MatrixPosition(7, 7) -> 2,
    MatrixPosition(3, 8) -> 1,
    MatrixPosition(14, 5) -> 1,
    MatrixPosition(11, 3) -> 1,
    MatrixPosition(5, 9) -> 3,
    MatrixPosition(13, 5) -> 3,
    MatrixPosition(3, 7) -> 2,
    MatrixPosition(8, 8) -> 3,
    MatrixPosition(8, 9) -> 1,
    MatrixPosition(9, 8) -> 2,
    MatrixPosition(13, 7) -> 1,
    MatrixPosition(2, 8) -> 1,
    MatrixPosition(10, 7) -> 5,
    MatrixPosition(8, 6) -> 2,
    MatrixPosition(12, 7) -> 3,
    MatrixPosition(1, 5) -> 1,
    MatrixPosition(10, 6) -> 2,
    MatrixPosition(13, 4) -> 2,
    MatrixPosition(13, 3) -> 2,
    MatrixPosition(12, 0) -> 1,
    MatrixPosition(11, 5) -> 3,
    MatrixPosition(6, 8) -> 4,
    MatrixPosition(8, 7) -> 2,
    MatrixPosition(15, 3) -> 1,
    MatrixPosition(0, 7) -> 1,
    MatrixPosition(6, 9) -> 2,
    MatrixPosition(1, 7) -> 3,
    MatrixPosition(9, 5) -> 3,
    MatrixPosition(14, 4) -> 1,
    MatrixPosition(3, 6) -> 2,
    MatrixPosition(3, 5) -> 1,
    MatrixPosition(11, 9) -> 3,
    MatrixPosition(5, 5) -> 1,
    MatrixPosition(0, 8) -> 1,
    MatrixPosition(0, 6) -> 1,
    MatrixPosition(11, 4) -> 2,
    MatrixPosition(5, 7) -> 2,
    MatrixPosition(7, 4) -> 1,
    MatrixPosition(6, 6) -> 1,
    MatrixPosition(7, 6) -> 3,
    MatrixPosition(12, 6) -> 1,
    MatrixPosition(11, 2) -> 1,
    MatrixPosition(12, 5) -> 2
  )
}
