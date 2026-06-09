package small.game

class TimerManager {
    private var remaining = 30

    fun remainingTime(): Int = remaining

    fun decrease() {
        if (remaining > 0) remaining--
    }

    fun isTimeOut(): Boolean = remaining == 0
}
