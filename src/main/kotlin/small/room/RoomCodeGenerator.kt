package small.room

object RoomCodeGenerator {
    private val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    fun generate(): String = (1..6).map { chars.random() }.joinToString("")
}
