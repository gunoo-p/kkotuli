package large.game

class GameService {
    private val allPlayers = listOf("Player1", "Player2", "Player3", "Player4")
    private val activePlayers = allPlayers.toMutableList()
    private val eliminated = mutableSetOf<String>()
    private var currentIndex = 0
    private var gameStatus = GameStatus.RUNNING
    private val history = mutableListOf<String>()
    private val usedWords = mutableSetOf<String>()
    private var lastWord: String? = null

    fun submitWord(word: String) {
        if (usedWords.contains(word)) throw DuplicateWordException("이미 사용된 단어입니다: $word")
        lastWord?.let {
            if (word.first() != it.last()) throw IllegalArgumentException("끝말잇기 규칙에 맞지 않습니다.")
        }
        usedWords.add(word)
        history.add(word)
        lastWord = word
        currentIndex = (currentIndex + 1) % activePlayers.size
    }

    fun currentPlayer(): String = activePlayers[currentIndex]

    fun eliminate(player: String) {
        eliminated.add(player)
        val idx = activePlayers.indexOf(player)
        activePlayers.remove(player)
        if (activePlayers.isNotEmpty()) {
            if (idx < currentIndex) currentIndex--
            if (currentIndex >= activePlayers.size) currentIndex = 0
        }
        if (activePlayers.size <= 1) gameStatus = GameStatus.FINISHED
    }

    fun timeoutCurrentPlayer() {
        val player = activePlayers[currentIndex]
        eliminate(player)
    }

    fun isEliminated(player: String): Boolean = eliminated.contains(player)

    fun activePlayers(): List<String> = activePlayers.toList()

    fun isGameOver(): Boolean = activePlayers.size <= 1

    fun isRunning(): Boolean = gameStatus == GameStatus.RUNNING

    fun getWinner(): String = activePlayers.first()

    fun status(): GameStatus = gameStatus

    fun getHistory(): List<String> = history.toList()
}
