package week1

fun addPath(mapString: String) = Map.fromString(mapString).addPath()

data class Coord(val x: Int, val y: Int) {
    val neighbours: List<Pair<Coord, Double>>
        get() = listOf(
                Pair(Coord(x - 1, y - 1), 1.5),
                Pair(Coord(x - 1, y), 1.0),
                Pair(Coord(x - 1, y + 1), 1.5),
                Pair(Coord(x, y - 1), 1.0),
                Pair(Coord(x, y + 1), 1.0),
                Pair(Coord(x + 1, y - 1), 1.5),
                Pair(Coord(x + 1, y), 1.0),
                Pair(Coord(x + 1, y + 1), 1.5)
        )

    infix fun distanceTo(coord: Coord) = Math.hypot((x - coord.x).toDouble(), (y - coord.y).toDouble())
}

data class Node(val coord: Coord, val heuristic: Double, val cost: Double, val cameFrom: Node?) {
    override fun hashCode() = coord.hashCode()
    override fun equals(other: Any?) = other is Node && coord == other.coord

    val path: Set<Coord>
        get() {
            tailrec fun path(node: Node? = this, curPath: Set<Coord> = emptySet()): Set<Coord> {
                if (node == null) return curPath
                return path(node.cameFrom, curPath + node.coord)
            }
            return path()
        }
}

infix fun <E> Set<E>.replace(e: E) = this - e + e
infix fun <E> Set<E>.replace(i: Iterable<E>) = i.fold(this) { acc, e -> acc replace e }

data class Map(val mapString: String, val tiles: Set<Coord>, val start: Coord, val end: Coord) {

    companion object {
        fun fromString(mapString: String) = mapString
                .split('\n')
                .foldIndexed(emptyList<Pair<Coord, Char>>()) { y, tiles, line ->
                    tiles + line.mapIndexed { x, c ->
                        Pair(Coord(x, y), c)
                    }
                }
                .fold(Map(mapString, emptySet(), Coord(-1, -1), Coord(-1, -1))) { map, (coord, c) ->
                    when (c) {
                        '.' -> map.copy(tiles = map.tiles + coord)
                        'S' -> map.copy(tiles = map.tiles + coord, start = coord)
                        'X' -> map.copy(tiles = map.tiles + coord, end = coord)
                        else -> map
                    }
                }
    }

    fun addPath() = mark(aStar())

    private fun neighbours(node: Node) = node.coord.neighbours
            .filter { (coord) -> coord in tiles }
            .map { (coord, cost) -> Node(coord, coord distanceTo end, node.cost + cost, node) }

    private tailrec fun aStar(
            open: Set<Node> = setOf(Node(start, start distanceTo end, 0.0, null)),
            closed: List<Coord> = emptyList(),
            cur: Node =
                    if (open.isEmpty()) throw RuntimeException("No path!")
                    else open.sortedBy { (_, heuristic, cost) -> heuristic + cost }.first()
    ): Set<Coord> = if (cur.coord == end) cur.path else {
        val newOpens = neighbours(cur)
                .filter { (coord) -> coord !in closed }
                .filter { (coord, _, cost) -> open.find { coord == it.coord }?.let { cost < it.cost } ?: true }
        aStar(open - cur replace newOpens, closed + cur.coord)
    }

    private fun mark(path: Set<Coord>) = mapString
            .split('\n')
            .mapIndexed { y, line ->
                line
                        .mapIndexed { x, c -> if (Coord(x, y) in path) '*' else c }
                        .joinToString(separator = "")
            }
            .joinToString(separator = "\n")
}
