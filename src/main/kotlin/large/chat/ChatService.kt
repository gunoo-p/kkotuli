package large.chat

class ChatService {
    private val messages = mutableListOf<Pair<String, String>>()

    fun send(sender: String, message: String) {
        messages.add(sender to message)
    }

    fun hasMessage(): Boolean = messages.isNotEmpty()

    fun getLastMessageFor(player: String): String = messages.last().second
}
