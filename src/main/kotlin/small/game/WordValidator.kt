package small.game

class InvalidWordException(message: String) : Exception(message)

object WordValidator {
    fun validate(previousWord: String, currentWord: String): Boolean {
        if (currentWord.length < 2) throw InvalidWordException("단어는 2글자 이상이어야 합니다.")
        if (currentWord.isBlank()) throw InvalidWordException("빈 단어는 허용되지 않습니다.")
        if (currentWord.first() != previousWord.last()) throw InvalidWordException("이전 단어의 끝 글자로 시작해야 합니다.")
        return true
    }
}
