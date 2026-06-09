package medium.dictionary

class DictionaryService {
    private val words = setOf(
        "사과", "과자", "자동차", "차표", "표범", "범주",
        "주사", "사자", "자전거", "거북이", "이상", "상자",
        "자유", "유리", "리듬", "듬직", "직업", "업무",
        "무지개", "개구리", "리본", "본인", "인형", "형제",
        "제목", "목사", "사탕", "탕수육", "육아", "아파트",
        "트럭", "럭비", "비행기", "기차", "차도", "도서관",
        "관심", "심장", "장미", "미소", "소나기", "기온"
    )

    fun exists(word: String): Boolean {
        if (word.isBlank()) return false
        return words.contains(word)
    }
}
