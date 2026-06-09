package large.room

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CreateRoomScenarioTest {

    @Test
    fun `사용자는 아바타를 선택하고 게임방을 생성할 수 있다`() {

        // given
        val roomService = RoomService()

        val avatar = Avatar(
            expression = "SMILE",
            color = "BLUE"
        )

        // when
        val room = roomService.createRoom(
            avatar = avatar
        )

        // then
        assertNotNull(
            room
        )

    }

    @Test
    fun `생성된 방에는 6자리 코드가 부여된다`() {

        // given
        val roomService = RoomService()

        // when
        val room = roomService.createRoom()

        // then
        assertEquals(
            6,
            room.code.length
        )

    }

    @Test
    fun `방 생성자는 방장이 된다`() {

        // given
        val roomService = RoomService()

        // when
        val room = roomService.createRoom()

        // then
        assertEquals(
            room.host,
            room.players.first()
        )

    }

    @Test
    fun `새로 생성된 방은 대기 상태이다`() {

        // given
        val roomService = RoomService()

        // when
        val room = roomService.createRoom()

        // then
        assertTrue(
            room.isWaiting()
        )

    }

}