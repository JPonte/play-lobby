package samurai

object Tile {
    type Tile = Int
    val Empty: Tile = 0
    val Sea: Tile = 1
    val Land: Tile = 2
    val Village: Tile = 3
    val City: Tile = 4
    val Edo: Tile = 5

    val Settlements: Set[Tile] = Set(Village, City, Edo)
}
