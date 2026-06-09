package small.game

class HistoryManager {
    private val history = mutableListOf<String>()

    fun addWord(word: String) {
        history.add(word)
    }

    fun getHistory(): List<String> = history.toList()
}
