package samurai
import Figure._

object Figure {
    type Figure = Int
    val Helmet: Figure = 0
    val Buddha: Figure = 1
    val RiceField: Figure = 2
}

case class FigureDeck(helmets: Int, buddhas: Int, riceFields: Int) {
    private val map = Map(Helmet -> helmets, Buddha -> buddhas, RiceField -> riceFields)

    def figureCount(figure: Figure): Int = map.getOrElse(figure, 0)

    def nonEmpty: Boolean = map.values.sum > 0
    def isEmpty: Boolean = map.values.sum == 0
}