package small.game

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WordValidatorTest {

    @Test
    fun `끝 글자와 첫 글자가 일치하면 단어 입력에 성공한다`() {

        // given
        val previousWord = "사과"
        val currentWord = "과자"

        // when
        val result = WordValidator.validate(
            previousWord,
            currentWord
        )

        // then
        assertTrue(result)

    }

    @Test
    fun `끝 글자와 첫 글자가 다르면 예외가 발생한다`() {

        assertFailsWith<InvalidWordException> {

            WordValidator.validate(
                "사과",
                "바다"
            )

        }

    }

    @Test
    fun `한 글자 단어를 입력하면 예외가 발생한다`() {

        assertFailsWith<InvalidWordException> {

            WordValidator.validate(
                "사과",
                "자"
            )

        }

    }

    @Test
    fun `빈 문자열을 입력하면 예외가 발생한다`() {

        assertFailsWith<InvalidWordException> {

            WordValidator.validate(
                "사과",
                ""
            )

        }

    }

}