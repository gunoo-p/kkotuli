package large.chat

class GameService {
    private val activePlayers = mutableListOf("Player1", "Player2", "Player3", "Player4")
    private var currentIndex = 0
    private val usedWords = mutableSetOf<String>()
    private var lastWord: String? = null
    private var running = true

    fun submitWord(word: String) {
        usedWords.add(word)
        lastWord = word
        currentIndex = (currentIndex + 1) % activePlayers.size
    }

    fun currentPlayer(): String = activePlayers[currentIndex]

    fun isRunning(): Boolean = running
}
