package large.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimeOutScenarioTest {

    @Test
    fun `제한 시간 내에 입력하지 못하면 플레이어가 탈락한다`() {

        // given
        val gameService = GameService()

        // when
        gameService.timeoutCurrentPlayer()

        // then
        assertTrue(
            gameService.isEliminated("Player1")
        )

    }

    @Test
    fun `탈락한 플레이어는 게임 참가자 목록에서 제외된다`() {

        // given
        val gameService = GameService()

        // when
        gameService.timeoutCurrentPlayer()

        // then
        assertFalse(
            gameService.activePlayers()
                .contains("Player1")
        )

    }

    @Test
    fun `시간 초과 후 다음 플레이어에게 턴이 넘어간다`() {

        // given
        val gameService = GameService()

        // when
        gameService.timeoutCurrentPlayer()

        // then
        assertEquals(
            "Player2",
            gameService.currentPlayer()
        )

    }

    @Test
    fun `한 명이 탈락해도 게임은 계속 진행된다`() {

        // given
        val gameService = GameService()

        // when
        gameService.timeoutCurrentPlayer()

        // then
        assertFalse(
            gameService.isGameOver()
        )

    }

}