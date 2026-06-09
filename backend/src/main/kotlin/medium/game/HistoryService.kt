package medium.game

class HistoryService {
    private val history = mutableListOf<String>()

    fun addWord(word: String) {
        history.add(word)
    }

    fun getHistory(): List<String> = history.toList()

    fun getHistoryFor(player: String): List<String> = history.toList()
}
