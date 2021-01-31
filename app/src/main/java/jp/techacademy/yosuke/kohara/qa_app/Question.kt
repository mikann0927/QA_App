package jp.techacademy.yosuke.kohara.qa_app

import java.io.Serializable
import kotlin.collections.ArrayList

//取得した質問のモデルクラスであるAnswerのArrayList
class Question(val title: String, val body: String, val name: String, val uid: String, val questionUid: String, val genre: Int, bytes: ByteArray, val answers: java.util.ArrayList<Answer>) : Serializable {
    val imageBytes: ByteArray

    init {
        imageBytes = bytes.clone()
    }
}