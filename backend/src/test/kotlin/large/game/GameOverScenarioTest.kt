package large.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameOverScenarioTest {

    @Test
    fun `마지막 한 명만 남으면 게임이 종료된다`() {

        // given
        val gameService = GameService()

        // when
        gameService.eliminate("Player1")
        gameService.eliminate("Player2")
        gameService.eliminate("Player3")

        // then
        assertTrue(
            gameService.isGameOver()
        )

    }

    @Test
    fun `마지막 생존 플레이어가 우승자가 된다`() {

        // given
        val gameService = GameService()

        gameService.eliminate("Player1")
        gameService.eliminate("Player2")
        gameService.eliminate("Player3")

        // when
        val winner = gameService.getWinner()

        // then
        assertEquals(
            "Player4",
            winner
        )

    }

    @Test
    fun `게임 종료 후 상태가 FINISHED가 된다`() {

        // given
        val gameService = GameService()

        // when
        gameService.eliminate("Player1")
        gameService.eliminate("Player2")
        gameService.eliminate("Player3")

        // then
        assertEquals(
            GameStatus.FINISHED,
            gameService.status()
        )

    }

    @Test
    fun `우승자는 생존 플레이어 목록에 존재한다`() {

        // given
        val gameService = GameService()

        gameService.eliminate("Player1")
        gameService.eliminate("Player2")
        gameService.eliminate("Player3")

        // when
        val winner = gameService.getWinner()

        // then
        assertTrue(
            gameService.activePlayers()
                .contains(winner)
        )

    }

}