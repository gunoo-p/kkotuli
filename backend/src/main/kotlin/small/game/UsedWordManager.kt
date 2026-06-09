package small.game

class DuplicateWordException(message: String) : Exception(message)

class UsedWordManager {
    private val words = mutableSetOf<String>()

    fun addWord(word: String) {
        if (words.contains(word)) throw DuplicateWordException("이미 사용된 단어입니다: $word")
        words.add(word)
    }

    fun contains(word: String): Boolean = words.contains(word)
}
