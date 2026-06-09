package small.nickname

object NicknameGenerator {
    private val verbs = listOf("달리는", "나는", "뛰는", "잠자는", "노래하는", "춤추는", "웃는", "우는")
    private val animals = listOf("호랑이", "사자", "코끼리", "펭귄", "여우", "토끼", "늑대", "곰")

    fun generate(): String = "${verbs.random()}${animals.random()}"
}
