package jp.techacademy.yosuke.kohara.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*
import android.util.Base64
import android.util.Log
import android.view.View
import com.google.firebase.database.ValueEventListener
import jp.techacademy.moe.hatori.qa_app_kotlin.Favorites

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference

    //課題対応2
    private var favoriteList = ArrayList<Favorites>()
    var  favoriteFlag = false

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

            // 渡ってきたQuestionのオブジェクトを保持する
            val extras = intent.extras
            mQuestion = extras!!.get("question") as Question

            title = mQuestion.title

        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser
        val dataBaseReference = FirebaseDatabase.getInstance().reference

        //課題対応　ログインしていなときはfavoritebuttonを消す。
        if( user == null) {
            favoritebutton.visibility = View.INVISIBLE
        } else {

            val favoriteRef = dataBaseReference.child(FavoritePATH).child(user.uid)

            //課題対応 Firebaseのfavoriteを読み込む
            favoriteRef.addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    //課題対応　Userのfavoriteをリストで保持する
                    val favoriteResult = snapshot.value as Map<String, String>?

                    if (favoriteResult != null){
                        for (key in favoriteResult.keys){
                            val temp = favoriteResult[key] as Map<String, String>
                            val favoriteGenre = temp["genre"] ?: ""
                            val favorite = Favorites(key,favoriteGenre)
                            favoriteList.add(favorite)

                        }
                    }

                    //お気に入りが登録されてたら★無ければ☆
                    if (favoriteList != null){
                        for ( i in favoriteList.indices){
                            if (favoriteList[i].uid == mQuestion.questionUid){
                                favoritebutton.setImageResource(R.drawable.ic_star_24)
                                favoriteFlag = true
                            }
                        }
                    }else {favoritebutton.setImageResource(R.drawable.ic_star_border_24)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })

            favoritebutton.apply{
                setOnClickListener {
                    if (!favoriteFlag) {
                        //課題対応押されたら一覧に登録する処理　Firebaseを更新する処理をかく
                        val data = HashMap<String, String>()
                        data["genre"] = mQuestion.genre.toString() ?: ""
                        data["title"] = mQuestion.title ?: ""
                        data["body"] = mQuestion.body ?: ""
                        data["name"] = mQuestion.name ?: ""
                        data["uid"] = mQuestion.uid ?: ""
                        val imageBytes = mQuestion.imageBytes
                        val bitmapString = Base64.encodeToString(imageBytes, Base64.DEFAULT)
                        data["image"] = bitmapString
                        favoriteRef.child(mQuestion.questionUid).setValue(data)
                        favoritebutton.setImageResource(R.drawable.ic_star_24)

                    }else{
                        favoriteRef.removeValue()
                        favoritebutton.setImageResource(R.drawable.ic_star_border_24)
                        Log.d("test","消したい")
                    }
                }
            }
        }

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)
    }



}
