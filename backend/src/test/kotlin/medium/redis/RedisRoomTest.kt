package medium.redis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RedisRoomTest {

    @Test
    fun `게임방 정보를 저장할 수 있다`() {

        // given
        val repository = RedisRoomRepository()

        val room = Room(
            code = "A1B2C3"
        )

        // when
        repository.save(room)

        // then
        val savedRoom = repository.findByCode("A1B2C3")

        assertEquals(
            "A1B2C3",
            savedRoom.code
        )

    }

    @Test
    fun `저장된 게임방 정보를 조회할 수 있다`() {

        // given
        val repository = RedisRoomRepository()

        val room = Room(
            code = "Q9W8E7"
        )

        repository.save(room)

        // when
        val foundRoom = repository.findByCode("Q9W8E7")

        // then
        assertEquals(
            "Q9W8E7",
            foundRoom.code
        )

    }

    @Test
    fun `게임방 정보를 삭제할 수 있다`() {

        // given
        val repository = RedisRoomRepository()

        val room = Room(
            code = "X1Y2Z3"
        )

        repository.save(room)

        // when
        repository.delete("X1Y2Z3")

        // then
        assertFailsWith<RoomNotFoundException> {

            repository.findByCode("X1Y2Z3")

        }

    }

}