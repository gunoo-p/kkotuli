package small.nickname

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NicknameGeneratorTest {

    @Test
    fun `닉네임을 생성할 수 있다`() {

        // when
        val nickname = NicknameGenerator.generate()

        // then
        assertTrue(
            nickname.isNotEmpty()
        )

    }

    @Test
    fun `생성된 닉네임은 빈 문자열이 아니다`() {

        // when
        val nickname = NicknameGenerator.generate()

        // then
        assertFalse(
            nickname.isBlank()
        )

    }

    @Test
    fun `닉네임은 동사와 동물 이름의 조합으로 생성된다`() {

        // when
        val nickname = NicknameGenerator.generate()

        // then
        assertTrue(
            nickname.length >= 2
        )

    }

    @Test
    fun `여러 번 생성해도 항상 문자열이 생성된다`() {

        repeat(100) {

            val nickname = NicknameGenerator.generate()

            assertTrue(
                nickname.isNotBlank()
            )

        }

    }

}