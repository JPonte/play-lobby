import io.circe.{KeyDecoder, KeyEncoder}

package object utils {

  case class MatrixPosition(column: Int, row: Int)
  type SparseMatrix[T] = Map[MatrixPosition, T]

  implicit val matrixPositionKeyEncoder: KeyEncoder[MatrixPosition] = new KeyEncoder[MatrixPosition] {
    override def apply(p: MatrixPosition): String = s"${p.column},${p.row}"
  }

  implicit val matrixPositionKeyDecoder: KeyDecoder[MatrixPosition] = new KeyDecoder[MatrixPosition] {
    override def apply(key: String): Option[MatrixPosition] = {
      val coords = key.split(",").map(_.toInt)
      Some(MatrixPosition(coords(0), coords(1)))
    }
  }
}
