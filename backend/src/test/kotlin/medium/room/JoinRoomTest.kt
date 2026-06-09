package medium.room

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JoinRoomTest {

    @Test
    fun `플레이어가 방에 입장할 수 있다`() {

        // given
        val roomService = RoomService()

        val roomCode = roomService.createRoom()

        // when
        val player = roomService.joinRoom(roomCode)

        // then
        assertTrue(
            roomService.findRoom(roomCode)
                .players
                .contains(player)
        )

    }

    @Test
    fun `입장 시 랜덤 닉네임이 생성된다`() {

        // given
        val roomService = RoomService()

        val roomCode = roomService.createRoom()

        // when
        val player = roomService.joinRoom(roomCode)

        // then
        assertNotNull(
            player.nickname
        )

    }

    @Test
    fun `최대 인원을 초과하면 예외가 발생한다`() {

        // given
        val roomService = RoomService()

        val roomCode = roomService.createRoom()

        roomService.joinRoom(roomCode)
        roomService.joinRoom(roomCode)
        roomService.joinRoom(roomCode)
        roomService.joinRoom(roomCode)

        // when & then
        assertFailsWith<RoomFullException> {

            roomService.joinRoom(roomCode)

        }

    }

    @Test
    fun `게임 진행 중인 방에는 입장할 수 없다`() {

        // given
        val roomService = RoomService()

        val roomCode = roomService.createRoom()

        roomService.startGame(roomCode)

        // when & then
        assertFailsWith<GameAlreadyStartedException> {

            roomService.joinRoom(roomCode)

        }

    }

}