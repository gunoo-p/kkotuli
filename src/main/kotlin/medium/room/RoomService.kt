package medium.room

class RoomNotFoundException(message: String) : Exception(message)
class RoomFullException(message: String) : Exception(message)
class GameAlreadyStartedException(message: String) : Exception(message)

data class Player(val nickname: String)

data class Room(
    val code: String,
    val players: MutableList<Player> = mutableListOf(),
    var started: Boolean = false
)

class RoomService {
    private val rooms = mutableMapOf<String, Room>()
    private val nicknames = listOf(
        "달리는호랑이", "나는사자", "뛰는코끼리", "잠자는펭귄",
        "노래하는여우", "춤추는토끼", "웃는늑대", "우는곰"
    )
    private var nicknameIndex = 0

    fun createRoom(): String {
        val code = generateCode()
        rooms[code] = Room(code)
        return code
    }

    fun findRoom(code: String): Room =
        rooms[code] ?: throw RoomNotFoundException("방을 찾을 수 없습니다: $code")

    fun deleteRoom(code: String) {
        rooms.remove(code) ?: throw RoomNotFoundException("방을 찾을 수 없습니다: $code")
    }

    fun joinRoom(code: String): Player {
        val room = findRoom(code)
        if (room.started) throw GameAlreadyStartedException("이미 게임이 시작된 방입니다.")
        if (room.players.size >= 4) throw RoomFullException("방이 꽉 찼습니다.")
        val player = Player(nicknames[nicknameIndex++ % nicknames.size])
        room.players.add(player)
        return player
    }

    fun startGame(code: String) {
        val room = findRoom(code)
        room.started = true
    }

    private fun generateCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
