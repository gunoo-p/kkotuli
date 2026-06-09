package large.integration

class RoomService {
    private val rooms = mutableMapOf<String, Room>()
    private val verbs = listOf("달리는", "나는", "뛰는", "잠자는")
    private val animals = listOf("호랑이", "사자", "코끼리", "펭귄")
    private var idx = 0

    fun createRoom(): Room {
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
        room.status = RoomStatus.RUNNING
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

class GameService {
    private val activePlayers = mutableListOf("Player1", "Player2", "Player3", "Player4")
    private var currentIndex = 0
    private val usedWords = mutableSetOf<String>()
    private var lastWord: String? = null

    fun submitWord(word: String) {
        usedWords.add(word)
        lastWord = word
        currentIndex = (currentIndex + 1) % activePlayers.size
    }

    fun currentPlayer(): String = activePlayers[currentIndex]

    fun eliminate(player: String) {
        val idx = activePlayers.indexOf(player)
        activePlayers.remove(player)
        if (activePlayers.isNotEmpty()) {
            if (idx < currentIndex) currentIndex--
            if (currentIndex >= activePlayers.size) currentIndex = 0
        }
    }

    fun isGameOver(): Boolean = activePlayers.size <= 1

    fun getWinner(): String = activePlayers.first()
}

class ChatService {
    private val messages = mutableListOf<Pair<String, String>>()

    fun send(sender: String, message: String) {
        messages.add(sender to message)
    }

    fun hasMessage(): Boolean = messages.isNotEmpty()

    fun getLastMessageFor(player: String): String = messages.last().second
}

enum class RoomStatus { WAITING, RUNNING }

data class Player(val nickname: String)

data class Room(
    val code: String,
    val host: Player,
    val players: MutableList<Player> = mutableListOf(),
    var status: RoomStatus = RoomStatus.WAITING
)
