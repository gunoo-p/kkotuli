package small.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimerManagerTest {

    @Test
    fun `타이머는 30초로 시작한다`() {

        // when
        val timerManager = TimerManager()

        // then
        assertEquals(
            30,
            timerManager.remainingTime()
        )

    }

    @Test
    fun `1초 감소할 수 있다`() {

        // given
        val timerManager = TimerManager()

        // when
        timerManager.decrease()

        // then
        assertEquals(
            29,
            timerManager.remainingTime()
        )

    }

    @Test
    fun `0초까지 감소할 수 있다`() {

        // given
        val timerManager = TimerManager()

        repeat(30) {
            timerManager.decrease()
        }

        // then
        assertEquals(
            0,
            timerManager.remainingTime()
        )

    }

    @Test
    fun `남은 시간이 0초이면 시간 초과 상태가 된다`() {

        // given
        val timerManager = TimerManager()

        repeat(30) {
            timerManager.decrease()
        }

        // then
        assertTrue(
            timerManager.isTimeOut()
        )

    }

}