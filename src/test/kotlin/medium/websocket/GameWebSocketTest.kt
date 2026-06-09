package medium.websocket

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameWebSocketTest {

    @Test
    fun `단어 입력 시 게임 상태가 브로드캐스트된다`() {

        // given
        val gameWebSocket = GameWebSocketHandler()

        // when
        gameWebSocket.broadcastGameState(
            word = "과자"
        )

        // then
        assertTrue(
            gameWebSocket.hasBroadcasted()
        )

    }

    @Test
    fun `모든 플레이어가 동일한 게임 상태를 수신한다`() {

        // given
        val gameWebSocket = GameWebSocketHandler()

        // when
        gameWebSocket.broadcastGameState(
            word = "자동차"
        )

        val player1State = gameWebSocket.getStateFor("Player1")
        val player2State = gameWebSocket.getStateFor("Player2")

        // then
        assertEquals(
            player1State,
            player2State
        )

    }

    @Test
    fun `턴 변경 정보가 모든 플레이어에게 전송된다`() {

        // given
        val gameWebSocket = GameWebSocketHandler()

        // when
        gameWebSocket.broadcastTurn(
            currentPlayer = "Player2"
        )

        // then
        assertEquals(
            "Player2",
            gameWebSocket.getTurnFor("Player1")
        )

        assertEquals(
            "Player2",
            gameWebSocket.getTurnFor("Player3")
        )

    }

}