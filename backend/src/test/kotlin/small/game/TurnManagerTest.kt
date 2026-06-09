package small.game

import kotlin.test.Test
import kotlin.test.assertEquals

class TurnManagerTest {

    @Test
    fun `첫 번째 플레이어가 현재 턴이 된다`() {

        // given
        val players = listOf(
            "Player1",
            "Player2",
            "Player3",
            "Player4"
        )

        // when
        val turnManager = TurnManager(players)

        // then
        assertEquals(
            "Player1",
            turnManager.currentPlayer()
        )
    }

    @Test
    fun `다음 플레이어로 턴이 이동한다`() {

        // given
        val players = listOf(
            "Player1",
            "Player2",
            "Player3"
        )

        val turnManager = TurnManager(players)

        // when
        turnManager.nextTurn()

        // then
        assertEquals(
            "Player2",
            turnManager.currentPlayer()
        )
    }

    @Test
    fun `마지막 플레이어 다음에는 첫 번째 플레이어로 돌아간다`() {

        // given
        val players = listOf(
            "Player1",
            "Player2",
            "Player3"
        )

        val turnManager = TurnManager(players)

        // when
        turnManager.nextTurn()
        turnManager.nextTurn()
        turnManager.nextTurn()

        // then
        assertEquals(
            "Player1",
            turnManager.currentPlayer()
        )
    }

    @Test
    fun `탈락한 플레이어는 건너뛴다`() {

        // given
        val players = listOf(
            "Player1",
            "Player2",
            "Player3",
            "Player4"
        )

        val turnManager = TurnManager(players)

        turnManager.eliminate("Player2")

        // when
        turnManager.nextTurn()

        // then
        assertEquals(
            "Player3",
            turnManager.currentPlayer()
        )
    }

}