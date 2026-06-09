package medium.websocket

class ChatWebSocketHandler {
    private var lastMessage: String? = null
    private var broadcasted = false

    fun broadcastMessage(sender: String, message: String) {
        lastMessage = message
        broadcasted = true
    }

    fun hasBroadcasted(): Boolean = broadcasted

    fun getLastMessageFor(player: String): String? = lastMessage

    fun isChatAvailable(): Boolean = true
}
