package medium.room

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class RoomServiceTest {

    @Test
    fun `방을 생성할 수 있다`() {

        // given
        val roomService = RoomService()

        // when
        val roomCode = roomService.createRoom()

        // then
        assertNotNull(roomCode)

    }

    @Test
    fun `생성된 방을 조회할 수 있다`() {

        // given
        val roomService = RoomService()

        val roomCode = roomService.createRoom()

        // when
        val room = roomService.findRoom(roomCode)

        // then
        assertEquals(
            roomCode,
            room.code
        )

    }

    @Test
    fun `방을 삭제할 수 있다`() {

        // given
        val roomService = RoomService()

        val roomCode = roomService.createRoom()

        // when
        roomService.deleteRoom(roomCode)

        // then
        assertFailsWith<RoomNotFoundException> {

            roomService.findRoom(roomCode)

        }

    }

    @Test
    fun `존재하지 않는 방을 조회하면 예외가 발생한다`() {

        // given
        val roomService = RoomService()

        // when & then
        assertFailsWith<RoomNotFoundException> {

            roomService.findRoom("AAAAAA")

        }

    }

}