package medium.redis

class RoomNotFoundException(message: String) : Exception(message)

data class Room(val code: String)

class RedisRoomRepository {
    private val store = mutableMapOf<String, Room>()

    fun save(room: Room) {
        store[room.code] = room
    }

    fun findByCode(code: String): Room =
        store[code] ?: throw RoomNotFoundException("방을 찾을 수 없습니다: $code")

    fun delete(code: String) {
        store.remove(code)
    }
}
