package small.game

class ResultManager(private val survivors: List<String>) {
    fun isGameOver(): Boolean = survivors.size <= 1
    fun getWinner(): String = survivors.first()
}
