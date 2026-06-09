package medium.websocket

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatWebSocketTest {

    @Test
    fun `채팅 메시지를 브로드캐스트할 수 있다`() {

        // given
        val chatWebSocket = ChatWebSocketHandler()

        // when
        chatWebSocket.broadcastMessage(
            sender = "Player2",
            message = "안녕하세요"
        )

        // then
        assertTrue(
            chatWebSocket.hasBroadcasted()
        )

    }

    @Test
    fun `모든 플레이어가 동일한 채팅 메시지를 수신한다`() {

        // given
        val chatWebSocket = ChatWebSocketHandler()

        // when
        chatWebSocket.broadcastMessage(
            sender = "Player2",
            message = "굿게임!"
        )

        val player1Message = chatWebSocket.getLastMessageFor("Player1")
        val player3Message = chatWebSocket.getLastMessageFor("Player3")

        // then
        assertEquals(
            player1Message,
            player3Message
        )

    }

    @Test
    fun `채팅 기능은 게임 진행과 독립적으로 동작한다`() {

        // given
        val chatWebSocket = ChatWebSocketHandler()

        // when
        chatWebSocket.broadcastMessage(
            sender = "Player4",
            message = "ㅋㅋㅋㅋ"
        )

        // then
        assertTrue(
            chatWebSocket.isChatAvailable()
        )

    }

}