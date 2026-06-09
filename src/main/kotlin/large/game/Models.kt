package large.game

enum class GameStatus { WAITING, RUNNING, FINISHED }

class DuplicateWordException(message: String) : Exception(message)

data class Avatar(val expression: String, val color: String)

data class Player(val nickname: String)

class Timer {
    private var running = false

    fun start() { running = true }
    fun isRunning(): Boolean = running
}

data class Room(
    val code: String,
    val host: Player,
    val players: MutableList<Player> = mutableListOf(),
    var status: GameStatus = GameStatus.WAITING,
    var currentPlayer: Player? = null,
    val timer: Timer = Timer()
) {
    fun isWaiting(): Boolean = status == GameStatus.WAITING
    fun isRunning(): Boolean = status == GameStatus.RUNNING
}

class RoomService {
    private val rooms = mutableMapOf<String, Room>()
    private val verbs = listOf("달리는", "나는", "뛰는", "잠자는")
    private val animals = listOf("호랑이", "사자", "코끼리", "펭귄")
    private var idx = 0

    fun createRoom(avatar: Avatar? = null): Room {
        val code = generateCode()
        val host = Player(nextNickname())
        val room = Room(code = code, host = host, players = mutableListOf(host))
        rooms[code] = room
        return room
    }

    fun joinRoom(code: String): Player {
        val room = rooms[code] ?: throw Exception("방을 찾을 수 없습니다.")
        val player = Player(nextNickname())
        room.players.add(player)
        return player
    }

    fun startGame(code: String) {
        val room = rooms[code] ?: throw Exception("방을 찾을 수 없습니다.")
        room.status = GameStatus.RUNNING
        room.currentPlayer = room.players.first()
        room.timer.start()
    }

    fun deleteRoom(code: String) {
        rooms.remove(code)
    }

    private fun nextNickname(): String = "${verbs[idx % verbs.size]}${animals[idx++ % animals.size]}"

    private fun generateCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
