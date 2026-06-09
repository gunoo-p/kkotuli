package large.room

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JoinRoomScenarioTest {

    @Test
    fun `사용자는 방 코드로 방에 입장할 수 있다`() {

        // given
        val roomService = RoomService()

        val room = roomService.createRoom()

        // when
        val player2 = roomService.joinRoom(
            room.code
        )

        // then
        assertEquals(
            2,
            room.players.size
        )

    }

    @Test
    fun `입장한 플레이어는 랜덤 닉네임을 부여받는다`() {

        // given
        val roomService = RoomService()

        val room = roomService.createRoom()

        // when
        val player2 = roomService.joinRoom(
            room.code
        )

        // then
        assertNotNull(
            player2.nickname
        )

    }

    @Test
    fun `플레이어가 입장하면 대기방 인원이 갱신된다`() {

        // given
        val roomService = RoomService()

        val room = roomService.createRoom()

        // when
        roomService.joinRoom(room.code)
        roomService.joinRoom(room.code)
        roomService.joinRoom(room.code)

        // then
        assertEquals(
            4,
            room.players.size
        )

    }

    @Test
    fun `최대 인원인 네 명까지 입장할 수 있다`() {

        // given
        val roomService = RoomService()

        val room = roomService.createRoom()

        // when
        roomService.joinRoom(room.code)
        roomService.joinRoom(room.code)
        roomService.joinRoom(room.code)

        // then
        assertEquals(
            4,
            room.players.size
        )

    }

}