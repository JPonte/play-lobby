package object utils {

  case class MatrixPosition(column: Int, row: Int)
  type SparseMatrix[T] = Map[MatrixPosition, T]
}
