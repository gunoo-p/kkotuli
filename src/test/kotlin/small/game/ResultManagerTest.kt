package small.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResultManagerTest {

    @Test
    fun `플레이어가 한 명만 남으면 게임이 종료된다`() {

        // given
        val resultManager = ResultManager(
            listOf("Player3")
        )

        // when & then
        assertTrue(
            resultManager.isGameOver()
        )

    }

    @Test
    fun `마지막까지 남은 플레이어가 승자가 된다`() {

        // given
        val resultManager = ResultManager(
            listOf("Player3")
        )

        // when
        val winner = resultManager.getWinner()

        // then
        assertEquals(
            "Player3",
            winner
        )

    }

    @Test
    fun `두 명 이상 남아있으면 게임은 종료되지 않는다`() {

        // given
        val resultManager = ResultManager(
            listOf(
                "Player1",
                "Player2"
            )
        )

        // when & then
        assertFalse(
            resultManager.isGameOver()
        )

    }

}