package small.game

class TurnManager(players: List<String>) {
    private val activePlayers = players.toMutableList()
    private var currentIndex = 0

    fun currentPlayer(): String = activePlayers[currentIndex]

    fun nextTurn() {
        currentIndex = (currentIndex + 1) % activePlayers.size
    }

    fun eliminate(player: String) {
        val idx = activePlayers.indexOf(player)
        activePlayers.remove(player)
        if (idx < currentIndex) currentIndex--
        if (currentIndex >= activePlayers.size) currentIndex = 0
    }
}
