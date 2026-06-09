package medium.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GameServiceTest {

    @Test
    fun `정상적인 단어를 입력할 수 있다`() {

        // given
        val gameService = GameService()

        // when
        val result = gameService.submitWord(
            previousWord = "사과",
            currentWord = "과자"
        )

        // then
        assertTrue(
            result
        )

    }

    @Test
    fun `사전에 없는 단어를 입력하면 예외가 발생한다`() {

        // given
        val gameService = GameService()

        // when & then
        assertFailsWith<WordNotFoundException> {

            gameService.submitWord(
                previousWord = "사과",
                currentWord = "과asdf"
            )

        }

    }

    @Test
    fun `이미 사용한 단어를 입력하면 예외가 발생한다`() {

        // given
        val gameService = GameService()

        gameService.submitWord(
            previousWord = "사과",
            currentWord = "과자"
        )

        // when & then
        assertFailsWith<DuplicateWordException> {

            gameService.submitWord(
                previousWord = "자동차",
                currentWord = "과자"
            )

        }

    }

    @Test
    fun `정상 입력 후 다음 플레이어로 턴이 이동한다`() {

        // given
        val gameService = GameService()

        // when
        gameService.submitWord(
            previousWord = "사과",
            currentWord = "과자"
        )

        // then
        assertEquals(
            "Player2",
            gameService.currentPlayer()
        )

    }

    @Test
    fun `플레이어가 한 명만 남으면 게임이 종료된다`() {

        // given
        val gameService = GameService()

        gameService.eliminate("Player1")
        gameService.eliminate("Player2")
        gameService.eliminate("Player4")

        // then
        assertTrue(
            gameService.isGameOver()
        )

    }

}