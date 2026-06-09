package medium.websocket

class GameWebSocketHandler {
    private var lastWord: String? = null
    private var currentTurn: String? = null
    private var broadcasted = false

    fun broadcastGameState(word: String) {
        lastWord = word
        broadcasted = true
    }

    fun hasBroadcasted(): Boolean = broadcasted

    fun getStateFor(player: String): String? = lastWord

    fun broadcastTurn(currentPlayer: String) {
        currentTurn = currentPlayer
    }

    fun getTurnFor(player: String): String? = currentTurn
}
