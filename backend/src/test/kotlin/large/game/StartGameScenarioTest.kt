package large.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StartGameScenarioTest {

    @Test
    fun `방장은 게임을 시작할 수 있다`() {

        // given
        val roomService = RoomService()

        val room = roomService.createRoom()

        roomService.joinRoom(room.code)
        roomService.joinRoom(room.code)
        roomService.joinRoom(room.code)

        // when
        roomService.startGame(room.code)

        // then
        assertTrue(
            room.isRunning()
        )

    }

    @Test
    fun `게임 시작 후 상태가 RUNNING으로 변경된다`() {

        // given
        val roomService = RoomService()

        val room = roomService.createRoom()

        roomService.joinRoom(room.code)
        roomService.joinRoom(room.code)
        roomService.joinRoom(room.code)

        // when
        roomService.startGame(room.code)

        // then
        assertEquals(
            GameStatus.RUNNING,
            room.status
        )

    }

    @Test
    fun `게임 시작 시 첫 번째 플레이어가 결정된다`() {

        // given
        val roomService = RoomService()

        val room = roomService.createRoom()

        roomService.joinRoom(room.code)
        roomService.joinRoom(room.code)
        roomService.joinRoom(room.code)

        // when
        roomService.startGame(room.code)

        // then
        assertNotNull(
            room.currentPlayer
        )

    }

    @Test
    fun `게임 시작과 동시에 제한 시간이 시작된다`() {

        // given
        val roomService = RoomService()

        val room = roomService.createRoom()

        roomService.joinRoom(room.code)
        roomService.joinRoom(room.code)
        roomService.joinRoom(room.code)

        // when
        roomService.startGame(room.code)

        // then
        assertTrue(
            room.timer.isRunning()
        )

    }

}