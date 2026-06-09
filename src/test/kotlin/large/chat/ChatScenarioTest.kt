package large.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatScenarioTest {

    @Test
    fun `게임 중 플레이어들은 채팅할 수 있다`() {

        // given
        val chatService = ChatService()

        // when
        chatService.send(
            sender = "Player3",
            message = "안녕하세요"
        )

        // then
        assertTrue(
            chatService.hasMessage()
        )

    }

    @Test
    fun `채팅 메시지는 모든 플레이어에게 전달된다`() {

        // given
        val chatService = ChatService()

        // when
        chatService.send(
            sender = "Player4",
            message = "ㅋㅋㅋㅋ"
        )

        // then
        assertEquals(
            "ㅋㅋㅋㅋ",
            chatService.getLastMessageFor("Player1")
        )

        assertEquals(
            "ㅋㅋㅋㅋ",
            chatService.getLastMessageFor("Player2")
        )

        assertEquals(
            "ㅋㅋㅋㅋ",
            chatService.getLastMessageFor("Player3")
        )

    }

    @Test
    fun `채팅 기능은 게임 진행에 영향을 주지 않는다`() {

        // given
        val gameService = GameService()
        val chatService = ChatService()

        // when
        chatService.send(
            sender = "Player2",
            message = "굿게임!"
        )

        // then
        assertTrue(
            gameService.isRunning()
        )

    }

    @Test
    fun `채팅 후에도 게임은 정상적으로 계속 진행된다`() {

        // given
        val gameService = GameService()
        val chatService = ChatService()

        // when
        chatService.send(
            sender = "Player1",
            message = "화이팅!"
        )

        gameService.submitWord("사과")

        // then
        assertEquals(
            "Player2",
            gameService.currentPlayer()
        )

    }

}