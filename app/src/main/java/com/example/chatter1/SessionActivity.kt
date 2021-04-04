package com.example.chatter1

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.session_content.*
import kotlinx.android.synthetic.main.session_one.view.*

class SessionActivity : AppCompatActivity() {

    //var m_sChatterID = "Chater"
    var m_sChatterID = "RumNet"

    //..... Result of unformatting m_sHttpBuffer
    var m_sHttpBuffer = ""
    var m_iError = 0
    var m_iSessions = 0                 //Current number of sessions in progress
    var m_iIpAddressClient = IntArray(4)
    var m_iPlayers = Array(MAX_SESSIONS, { IntArray(MAX_MEMBERS) })
    //Number of participants for each session
    //var m_sPlayerName = Array<Array<String?>>(MAX_SESSIONS) { arrayOfNulls(MAX_MEMBERS) }
    var m_sPlayerName = Array(MAX_SESSIONS) {Array(MAX_MEMBERS){""} }
    //Participant names
    var m_iHostIpAddress = Array(MAX_SESSIONS, { IntArray(4) })

    //var m_sessionTable = MutableList(10) {SessionRecord()}
    var m_sessionTable = MutableList(MAX_SESSIONS) {SessionRecord()}

    lateinit var layoutManager: RecyclerView.LayoutManager
    lateinit var adapter: SessionRecyclerViewAdapter

    lateinit var netLogin: NetLogin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session)

        //..... Instantiate netLogin
        netLogin = NetLogin()
        //buildSessionTable()

    }

    override fun onStart() {

        super.onStart()

        layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        adapter = SessionRecyclerViewAdapter(m_sessionTable)
        recyclerView.adapter = this.adapter
    }

    fun buildSessionTable () {

        //..... getSessionInfo
        netLogin.getSessionInfo(this, m_sChatterID)

    }

    fun onClickButtonAction (view : View) {

        val sAction = view.buttonSessionAction.text
        val sSessionGroup = view.textSessionGroup.text
        var iSessionGroup = sSessionGroup.toString().toInt()

        //..... Is this to Start a Chatter session?
        if (sAction == R.string.button_text_start.toString()) {


        }

        //..... 2021/03/20: The intent.puExtra () requires .toString when passing the sNickName
        //      Otherwise the onActivityResult() in MainActivity will get null on sNickname.
        //      In both Olymoic3 & Kotlin14C, the string data were converted using .toString() prior to putExtra().
        //      This solution was found in Stack Overflow. The URL is below
        //          https://stackoverflow.com/questions/15555750/android-intent-getstringextra-returns-null
        //
        //..... Return the Nickname to the Activity that called me
//        val intent = Intent()
//        intent.putExtra (NICKNAME_KEY, sNickname.toString())
//        setResult (REQUEST_CODE_GET_NICKNAME, intent)
//        finish ()

    }

    fun startSession () {

    }
}