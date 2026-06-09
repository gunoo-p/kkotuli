package medium.game

import medium.dictionary.DictionaryService

class WordNotFoundException(message: String) : Exception(message)
class DuplicateWordException(message: String) : Exception(message)

class GameService {
    private val activePlayers = mutableListOf("Player1", "Player2", "Player3", "Player4")
    private var currentIndex = 0
    private val usedWords = mutableSetOf<String>()
    private val dictionary = DictionaryService()

    fun submitWord(previousWord: String, currentWord: String): Boolean {
        if (usedWords.contains(currentWord)) throw DuplicateWordException("이미 사용된 단어입니다: $currentWord")
        if (!dictionary.exists(currentWord)) throw WordNotFoundException("사전에 없는 단어입니다: $currentWord")
        usedWords.add(currentWord)
        currentIndex = (currentIndex + 1) % activePlayers.size
        return true
    }

    fun currentPlayer(): String = activePlayers[currentIndex]

    fun eliminate(player: String) {
        val idx = activePlayers.indexOf(player)
        activePlayers.remove(player)
        if (idx < currentIndex) currentIndex--
        if (currentIndex >= activePlayers.size) currentIndex = 0
    }

    fun isGameOver(): Boolean = activePlayers.size <= 1
}
