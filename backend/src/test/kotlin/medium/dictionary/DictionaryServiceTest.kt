package medium.dictionary

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DictionaryServiceTest {

    @Test
    fun `존재하는 단어는 true를 반환한다`() {

        // given
        val dictionaryService = DictionaryService()

        // when
        val result = dictionaryService.exists("사과")

        // then
        assertTrue(
            result
        )

    }

    @Test
    fun `존재하지 않는 단어는 false를 반환한다`() {

        // given
        val dictionaryService = DictionaryService()

        // when
        val result = dictionaryService.exists("asdf")

        // then
        assertFalse(
            result
        )

    }

    @Test
    fun `빈 문자열은 단어로 인정하지 않는다`() {

        // given
        val dictionaryService = DictionaryService()

        // when
        val result = dictionaryService.exists("")

        // then
        assertFalse(
            result
        )

    }

}