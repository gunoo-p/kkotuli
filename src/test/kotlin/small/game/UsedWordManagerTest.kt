package small.game

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class UsedWordManagerTest {

    @Test
    fun `새로운 단어를 저장할 수 있다`() {

        // given
        val manager = UsedWordManager()

        // when
        manager.addWord("사과")

        // then
        assertTrue(
            manager.contains("사과")
        )

    }

    @Test
    fun `저장되지 않은 단어는 사용한 단어가 아니다`() {

        // given
        val manager = UsedWordManager()

        manager.addWord("사과")

        // then
        assertFalse(
            manager.contains("과자")
        )

    }

    @Test
    fun `이미 사용한 단어를 다시 입력하면 예외가 발생한다`() {

        // given
        val manager = UsedWordManager()

        manager.addWord("사과")

        // when & then
        assertFailsWith<DuplicateWordException> {

            manager.addWord("사과")

        }

    }

}