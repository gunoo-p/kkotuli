package large.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EndToEndScenarioTest {

    @Test
    fun `게임 전체 시나리오가 정상적으로 진행된다`() {

        // given
        val roomService = RoomService()
        val gameService = GameService()
        val chatService = ChatService()

        // Player1 방 생성
        val room = roomService.createRoom()

        // Player2 ~ Player4 입장
        roomService.joinRoom(room.code)
        roomService.joinRoom(room.code)
        roomService.joinRoom(room.code)

        // 게임 시작
        roomService.startGame(room.code)

        // 끝말잇기 진행
        gameService.submitWord("사과")
        gameService.submitWord("과자")
        gameService.submitWord("자동차")
        gameService.submitWord("차표")

        // 채팅
        chatService.send(
            sender = "Player3",
            message = "굿게임!"
        )

        // Player2 시간 초과
        gameService.eliminate("Player2")

        // Player1 탈락
        gameService.eliminate("Player1")

        // Player3 탈락
        gameService.eliminate("Player3")

        // then

        // Player4 우승
        assertEquals(
            "Player4",
            gameService.getWinner()
        )

        // 게임 종료
        assertTrue(
            gameService.isGameOver()
        )

        // 방 삭제
        roomService.deleteRoom(room.code)

    }

}