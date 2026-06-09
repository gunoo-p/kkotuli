package small.chat

class InvalidChatMessageException(message: String) : Exception(message)

class ChatMessage(val content: String) {
    init {
        if (content.isBlank()) throw InvalidChatMessageException("빈 메시지는 허용되지 않습니다.")
        if (content.length > 100) throw InvalidChatMessageException("메시지는 100자를 초과할 수 없습니다.")
    }
}
