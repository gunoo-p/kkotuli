package small.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HistoryManagerTest {

    @Test
    fun `단어를 히스토리에 저장할 수 있다`() {

        // given
        val historyManager = HistoryManager()

        // when
        historyManager.addWord("사과")

        // then
        assertTrue(
            historyManager.getHistory()
                .contains("사과")
        )

    }

    @Test
    fun `단어 입력 순서를 유지한다`() {

        // given
        val historyManager = HistoryManager()

        // when
        historyManager.addWord("사과")
        historyManager.addWord("과자")
        historyManager.addWord("자동차")

        // then
        assertEquals(
            listOf(
                "사과",
                "과자",
                "자동차"
            ),
            historyManager.getHistory()
        )

    }

    @Test
    fun `전체 히스토리를 조회할 수 있다`() {

        // given
        val historyManager = HistoryManager()

        historyManager.addWord("사과")
        historyManager.addWord("과자")

        // when
        val history = historyManager.getHistory()

        // then
        assertEquals(
            2,
            history.size
        )

        assertEquals(
            "사과",
            history[0]
        )

        assertEquals(
            "과자",
            history[1]
        )

    }

}