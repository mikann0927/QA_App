package jp.techacademy.yosuke.kohara.qa_app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar  // ← 追加
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.moe.hatori.qa_app_kotlin.Favorites
// findViewById()を呼び出さずに該当Viewを取得するために必要となるインポート宣言
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.app_bar_main.fab
import kotlinx.android.synthetic.main.content_main.listView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var mToolbar: Toolbar? = null
    private var mGenre = 0

    private lateinit var mDatabaseReference: DatabaseReference
    //Firebaseに書き込むために必要なクラス
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter
    //質問画面一覧のために必要なQuestionクラスのリストとアダプタ

    private var mGenreRef: DatabaseReference? = null

    //private var favoriteList = ArrayList<Favorites>()

    //questionListAdapterへのデータの設定
    private val mEventListener = object : ChildEventListener {

        @RequiresApi(Build.VERSION_CODES.FROYO)
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {

            val map = dataSnapshot.value as Map<String, String>
            Log.d("Test1","")
            val title = map["title"] ?: ""
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""
            val imageString = map["image"] ?: ""
            val bytes =
                if (imageString.isNotEmpty()) {
                    Base64.decode(imageString, Base64.DEFAULT)
                } else {
                    byteArrayOf()
                }

            val answerArrayList = ArrayList<Answer>()
            val answerMap = map["answers"] as Map<String, String>?
            if (answerMap != null) {
                for (key in answerMap.keys) {
                    val temp = answerMap[key] as Map<String, String>
                    val answerBody = temp["body"] ?: ""
                    val answerName = temp["name"] ?: ""
                    val answerUid = temp["uid"] ?: ""
                    val answer = Answer(answerBody, answerName, answerUid, key)
                    answerArrayList.add(answer)
                }
            }

            val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
                mGenre, bytes, answerArrayList)
            mQuestionArrayList.add(question)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            // 変更があったQuestionを探す
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // このアプリで変更がある可能性があるのは回答（Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }

                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) {

        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {

        }

        override fun onCancelled(p0: DatabaseError) {

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // idがtoolbarがインポート宣言により取得されているので
        // id名でActionBarのサポートを依頼
        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)

        // fabにClickリスナーを登録
        fab.setOnClickListener { view ->
            // ジャンルを選択していない場合（mGenre == 0）はエラーを表示するだけ
            if (mGenre == 0) {
                Snackbar.make(view, getString(R.string.question_no_select_genre), Snackbar.LENGTH_LONG).show()
            }

            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // ジャンルを渡して質問作成画面を起動する
                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                intent.putExtra("genre", mGenre)
                startActivity(intent)
            }
        }

        // ナビゲーションドロワーの設定
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout) //activity_main.xml
        val toggle = ActionBarDrawerToggle(this, drawer, mToolbar, R.string.app_name, R.string.app_name)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        listView.setOnItemClickListener{parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        //課題対応 1:趣味を既定の選択とする
        if(mGenre == 0) {
            onNavigationItemSelected(nav_view.menu.getItem(0))
        }

        //課題対応　ログインしているときは「お気に入り」を表示
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.getMenu().clear()
        val user_login = FirebaseAuth.getInstance().currentUser
        if ( user_login != null){
            navigationView.inflateMenu(R.menu.activity_main_drawer)
            navigationView.inflateMenu(R.menu.activity_main_logindrawer)
        }else{
            navigationView.inflateMenu(R.menu.activity_main_drawer)
        }
        navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val user = FirebaseAuth.getInstance().currentUser

        if (id == R.id.nav_hobby) {
            toolbar.title = getString(R.string.menu_hobby_label)
            mGenre = 1
            fab.visibility = View.VISIBLE
        } else if (id == R.id.nav_life) {
            toolbar.title = getString(R.string.menu_life_label)
            fab.visibility = View.VISIBLE
            mGenre = 2
        } else if (id == R.id.nav_health) {
            toolbar.title = getString(R.string.menu_health_label)
            mGenre = 3
            fab.visibility = View.VISIBLE
        } else if (id == R.id.nav_compter) {
            toolbar.title = getString(R.string.menu_compter_label)
            mGenre = 4
            fab.visibility = View.VISIBLE
        } else if (id == R.id.nav_favorite){
            toolbar.title = getString(R.string.menu_favorite_label)

            //課題対応　favorite追加　質問ボタン消す
            mGenre = 5
            fab.visibility = View.INVISIBLE
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        listView.adapter = mAdapter

        // 選択したジャンルにリスナーを登録する
        if (mGenreRef != null) {
            mGenreRef!!.removeEventListener(mEventListener)
        }
        //課題対応 お気に入りが選択されたらmGenreRefを変更
        if ( mGenre != 5){

            mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())
            mGenreRef!!.addChildEventListener(mEventListener)
            if ( user != null ){
                val favoriteRef = mDatabaseReference.child(FavoritePATH).child(user.uid)
                favoriteRef!!.removeEventListener(favEventListener)
            }

        } else {
            if(user != null){
                //課題対応　favoritesを読み込んでリストを作成する
                val favoriteRef = mDatabaseReference.child(FavoritePATH).child(user.uid)

                favoriteRef.addValueEventListener(favEventListener)

            }
        }

        return true
    }

    private  val favEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            //課題対応　読み込んできたログインUserのfavoriteをリストで保持
            val favoriteResult = snapshot.value as Map<String, String>?

            if (favoriteResult != null){
                for (key in favoriteResult.keys){

                    val temp = favoriteResult[key] as Map<String, String>
                    val favoriteGenre = temp["genre"] ?: ""
                    //val favorite = Favorites(key,favoriteGenre)

                    mDatabaseReference.child(ContentsPATH).child(favoriteGenre).child(key).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val map = snapshot.value as Map<String, String>
                            val title = map["title"] ?: ""
                            val body = map["body"] ?: ""
                            val name = map["name"] ?: ""
                            val uid = map["uid"] ?: ""
                            val imageString = map["image"] ?: ""
                            val bytes =
                                if (imageString.isNotEmpty()) {
                                    Base64.decode(imageString, Base64.DEFAULT)
                                } else {
                                    byteArrayOf()
                                }

                            val answerArrayList = ArrayList<Answer>()
                            val answerMap = map["answers"] as Map<String, String>?
                            if (answerMap != null) {
                                for (key in answerMap.keys) {
                                    val temp = answerMap[key] as Map<String, String>
                                    val answerBody = temp["body"] ?: ""
                                    val answerName = temp["name"] ?: ""
                                    val answerUid = temp["uid"] ?: ""
                                    val answer = Answer(answerBody, answerName, answerUid, key)
                                    answerArrayList.add(answer)
                                }
                            }

                            val question = Question(title, body, name, uid, snapshot.key ?: "",
                                favoriteGenre.toInt(), bytes, answerArrayList)

                            mQuestionArrayList.add(question)
                            mAdapter.notifyDataSetChanged()
                        }
                        override fun onCancelled(firebaseError: DatabaseError) {}
                    })
                }
            }

        }

        override fun onCancelled(error: DatabaseError) {
        }

    }




}
