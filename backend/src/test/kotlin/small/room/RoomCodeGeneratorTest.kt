package small.room

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomCodeGeneratorTest {

    @Test
    fun `방 코드를 생성할 수 있다`() {

        // when
        val roomCode = RoomCodeGenerator.generate()

        // then
        assertTrue(
            roomCode.isNotBlank()
        )

    }

    @Test
    fun `방 코드는 6자리이다`() {

        // when
        val roomCode = RoomCodeGenerator.generate()

        // then
        assertEquals(
            6,
            roomCode.length
        )

    }

    @Test
    fun `방 코드는 대문자와 숫자로만 이루어진다`() {

        // when
        val roomCode = RoomCodeGenerator.generate()

        // then
        assertTrue(
            roomCode.matches(
                Regex("[A-Z0-9]{6}")
            )
        )

    }

    @Test
    fun `여러 번 생성해도 항상 올바른 형식을 유지한다`() {

        repeat(1000) {

            val roomCode = RoomCodeGenerator.generate()

            assertTrue(
                roomCode.matches(
                    Regex("[A-Z0-9]{6}")
                )
            )

        }

    }

}