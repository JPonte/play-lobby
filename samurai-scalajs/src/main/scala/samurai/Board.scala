package samurai

import samurai.Tile._
import Board._

object Board {
  type Board = Seq[Seq[Tile]]

  val twoPlayerBoard: Seq[Seq[Tile]] = Seq(
    Seq(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1),
    Seq(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 3, 1),
    Seq(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 2, 1),
    Seq(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 4, 2, 3, 1),
    Seq(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 1),
    Seq(0, 1, 1, 1, 1, 1, 1, 1, 2, 3, 2, 3, 2, 3, 1),
    Seq(1, 2, 3, 2, 2, 3, 1, 3, 2, 2, 2, 2, 1, 1),
    Seq(1, 3, 2, 2, 3, 2, 2, 2, 2, 2, 5, 2, 3, 1),
    Seq(1, 1, 1, 1, 1, 2, 4, 2, 3, 2, 2, 2, 1),
    Seq(0, 0, 0, 0, 1, 3, 2, 1, 1, 1, 1, 3, 1),
    Seq(0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1)
  )

  type SparseMatrix[T] = Map[(Int, Int), T]

  val board2: SparseMatrix[Tile] = Map(
    (5, 6) -> 3,
    (9, 6) -> 2,
    (11, 10) -> 1,
    (13, 2) -> 2,
    (4, 8) -> 1,
    (12, 1) -> 1,
    (8, 5) -> 2,
    (10, 8) -> 2,
    (10, 5) -> 2,
    (11, 8) -> 2,
    (14, 1) -> 1,
    (9, 9) -> 1,
    (10, 10) -> 1,
    (7, 8) -> 2,
    (11, 6) -> 2,
    (7, 5) -> 1,
    (4, 7) -> 3,
    (7, 9) -> 1,
    (2, 5) -> 1,
    (6, 10) -> 1,
    (14, 3) -> 3,
    (2, 6) -> 3,
    (8, 4) -> 1,
    (13, 1) -> 3,
    (10, 9) -> 1,
    (9, 7) -> 2,
    (1, 8) -> 1,
    (14, 2) -> 1,
    (6, 5) -> 1,
    (4, 5) -> 1,
    (4, 10) -> 1,
    (5, 8) -> 2,
    (13, 0) -> 1,
    (4, 6) -> 2,
    (12, 2) -> 2,
    (5, 10) -> 1,
    (12, 3) -> 4,
    (12, 8) -> 1,
    (11, 7) -> 2,
    (6, 7) -> 2,
    (4, 9) -> 1,
    (2, 7) -> 2,
    (10, 4) -> 1,
    (9, 4) -> 1,
    (12, 4) -> 2,
    (13, 6) -> 1,
    (12, 9) -> 1,
    (1, 6) -> 2,
    (7, 7) -> 2,
    (3, 8) -> 1,
    (14, 5) -> 1,
    (11, 3) -> 1,
    (5, 9) -> 3,
    (13, 5) -> 3,
    (3, 7) -> 2,
    (8, 8) -> 3,
    (8, 9) -> 1,
    (9, 8) -> 2,
    (13, 7) -> 1,
    (2, 8) -> 1,
    (10, 7) -> 5,
    (8, 6) -> 2,
    (12, 7) -> 3,
    (1, 5) -> 1,
    (10, 6) -> 2,
    (13, 4) -> 2,
    (13, 3) -> 2,
    (12, 0) -> 1,
    (11, 5) -> 3,
    (6, 8) -> 4,
    (8, 7) -> 2,
    (15, 3) -> 1,
    (0, 7) -> 1,
    (6, 9) -> 2,
    (1, 7) -> 3,
    (9, 5) -> 3,
    (14, 4) -> 1,
    (3, 6) -> 2,
    (3, 5) -> 1,
    (11, 9) -> 3,
    (5, 5) -> 1,
    (0, 8) -> 1,
    (0, 6) -> 1,
    (11, 4) -> 2,
    (5, 7) -> 2,
    (7, 4) -> 1,
    (6, 6) -> 1,
    (7, 6) -> 3,
    (12, 6) -> 1,
    (11, 2) -> 1,
    (12, 5) -> 2
  )

}
