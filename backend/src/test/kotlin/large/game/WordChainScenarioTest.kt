package large.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WordChainScenarioTest {

    @Test
    fun `플레이어들은 정상적으로 끝말잇기를 진행할 수 있다`() {

        // given
        val gameService = GameService()

        // when
        gameService.submitWord("사과")
        gameService.submitWord("과자")
        gameService.submitWord("자동차")
        gameService.submitWord("차표")

        // then
        assertTrue(
            gameService.getHistory().contains("차표")
        )

    }

    @Test
    fun `입력된 단어들은 히스토리에 저장된다`() {

        // given
        val gameService = GameService()

        // when
        gameService.submitWord("사과")
        gameService.submitWord("과자")
        gameService.submitWord("자동차")

        // then
        assertEquals(
            listOf(
                "사과",
                "과자",
                "자동차"
            ),
            gameService.getHistory()
        )

    }

    @Test
    fun `단어 입력 후 턴이 다음 플레이어로 이동한다`() {

        // given
        val gameService = GameService()

        // when
        gameService.submitWord("사과")

        // then
        assertEquals(
            "Player2",
            gameService.currentPlayer()
        )

    }

    @Test
    fun `이미 사용한 단어는 다시 사용할 수 없다`() {

        // given
        val gameService = GameService()

        gameService.submitWord("사과")
        gameService.submitWord("과자")

        // when & then
        assertFailsWith<DuplicateWordException> {

            gameService.submitWord("사과")

        }

    }

}