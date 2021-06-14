package com.example.chatter1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.session_content.*
import kotlinx.android.synthetic.main.session_one.view.*

class SessionActivity : AppCompatActivity() {

    //var m_sChatterID = "Chater"
    var m_sChatterID = "RumNet"

    lateinit var netViewModel : NetViewModel

    //..... Result of unformatting m_sHttpBuffer
    var m_sHttpBuffer = ""
    var m_iError = 0
    var m_iSessions = 0                 //Current number of sessions in progress

    //var m_sessionTable = MutableList(MAX_SESSIONS) {SessionRecord()}
    var m_sessionTable : MutableList<SessionRecord> = mutableListOf<SessionRecord>()

    lateinit var layoutManager: RecyclerView.LayoutManager
    lateinit var adapter: SessionRecyclerViewAdapter

//    val netLogin = NetLogin()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session)

        netViewModel = ViewModelProvider(this).get(NetViewModel::class.java)

        //..... Get HttpBuffer & build Realm Session Table
        m_sHttpBuffer = intent.getStringExtra(HTTP_BUFFER).toString()
        buildSessionTable(m_sHttpBuffer)

    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //      buildSessionTable (sBuffer) creates Realm DB from the Session data from internet buffer
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    fun buildSessionTable (sHttpBuffer: String) {

        //..... First, unformat the HTTP Buffer
        //netViewModel.unformatSessionData(m_sHttpBuffer)
        netViewModel.unformatSessionData(sHttpBuffer)

        //realm = Realm.getDefaultInstance()

        //..... Copy SessionTable from ViewModel to this activity
        copySessionTable()
        //..... Add a blank entry at the end
        addNewSessionEntry()
    }


    fun copySessionTable() {

        //..... Since netViewModel has never been initialized, do it here
        //netViewModel = NetViewModel()

        m_iError = netViewModel.m_iError
        m_iSessions = netViewModel.m_iSessions
        var i = 0
        while (i < m_iSessions) {
            val sessionRecord = netViewModel.m_sessionTable[i]
            m_sessionTable.add (sessionRecord)
            i++
        }
    }

    fun addNewSessionEntry() {

        //..... Since netViewModel has never been initialized, do it here
        //netViewModel = NetViewModel()

        val sessionRecord = SessionRecord()
        sessionRecord.sessionGroupId = m_iSessions + 1
        m_sessionTable.add (sessionRecord)
    }

    override fun onStart() {

        super.onStart()

        layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        adapter = SessionRecyclerViewAdapter(m_sessionTable)
        recyclerView.adapter = this.adapter
    }

    fun onClickButtonAction (view : View) {

        //..... Find which action type
        val sButtonText = view.buttonSessionAction.text

        //..... Do nothing if Full
        val sFull = getString(R.string.button_text_full)
        if (sButtonText == sFull )
            return

        val intent = Intent()
        intent.putExtra (RETURN_DATA_SESSION_ACTION_KEY, sButtonText)
        setResult (REQUEST_CODE_GET_SESSION_INFORMATION, intent)
        finish ()
    }

    fun onClickButtonQuit (view : View) {

        //..... Find which action type
        val sButtonText = view.buttonSessionQuit.text

        val intent = Intent()
        intent.putExtra (RETURN_DATA_SESSION_ACTION_KEY, sButtonText)
        setResult (REQUEST_CODE_GET_SESSION_INFORMATION, intent)
        finish ()
    }
}