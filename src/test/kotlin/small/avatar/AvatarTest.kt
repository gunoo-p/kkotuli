package small.avatar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AvatarTest {

    @Test
    fun `표정과 색상을 선택하여 아바타를 생성할 수 있다`() {

        // given
        val expression = 5
        val color = 12

        // when
        val avatar = Avatar(
            expression = expression,
            color = color
        )

        // then
        assertEquals(5, avatar.expression)
        assertEquals(12, avatar.color)

    }

    @Test
    fun `표정 번호가 15보다 크면 예외가 발생한다`() {

        assertFailsWith<InvalidAvatarException> {

            Avatar(
                expression = 16,
                color = 5
            )

        }

    }

    @Test
    fun `색상 번호가 15보다 크면 예외가 발생한다`() {

        assertFailsWith<InvalidAvatarException> {

            Avatar(
                expression = 3,
                color = 16
            )

        }

    }

    @Test
    fun `음수 값이 들어오면 예외가 발생한다`() {

        assertFailsWith<InvalidAvatarException> {

            Avatar(
                expression = -1,
                color = 5
            )

        }

    }

}