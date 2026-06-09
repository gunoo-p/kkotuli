package medium.game

import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryServiceTest {

    @Test
    fun `단어 입력 시 히스토리에 저장된다`() {

        // given
        val historyService = HistoryService()

        // when
        historyService.addWord("사과")
        historyService.addWord("과자")

        // then
        assertEquals(
            listOf(
                "사과",
                "과자"
            ),
            historyService.getHistory()
        )

    }

    @Test
    fun `모든 플레이어는 동일한 히스토리를 조회할 수 있다`() {

        // given
        val historyService = HistoryService()

        historyService.addWord("사과")
        historyService.addWord("과자")
        historyService.addWord("자동차")

        // when
        val player1History = historyService.getHistoryFor("Player1")
        val player2History = historyService.getHistoryFor("Player2")

        // then
        assertEquals(
            player1History,
            player2History
        )

    }

}