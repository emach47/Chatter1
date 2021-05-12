package com.example.chatter1

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager

const val NICKNAME_KEY = "Nickname"
//!!!!! 2021/03/28: TODO: Returning from NicknameActivity has a problem.
//      The User can change Nickname any time by selecting the Change Nickname menu option.
//      However, this app assumes that is done only on the start of the app.
//      Is it OK to change the Nickname anytime, even if during the Chatter session?
//      Or the app force the User to confirm the Nickname he/she first created?
const val REQUEST_CODE_GET_NICKNAME = 1234
const val REQUEST_CODE_GET_SESSION_INFORMATION = 200

//..... HTTP operation request code
const val HTTP_REQUEST_SESSION_INFORMARION    = 1
const val HTTP_REQUEST_ADD_HOST               = 2
const val HTTP_REQUEST_REMOVE_HOST            = 3
const val HTTP_REQUEST_ADD_GUEST              = 4
const val HTTP_REQUEST_REMOVE_GUEST           = 5


class MainActivity : AppCompatActivity() {

    //private lateinit var realm: Realm

    //..... Define Session ID
    val m_sSessionID = "RumNet"

    //..... Properties for NicknameActivity
    var m_sNickname =""

    //=============================================================================================
    //  Properties for NetLogin
    //=============================================================================================
    //..... returned HTTP Buffer
    //lateinit var m_sHttpBuffer : String
    //var m_iHttpRequestCode = HTTP_REQUEST_SESSION_INFORMARION

    var m_sHttpBuffer = ""

    lateinit var netViewModel : NetViewModel

    lateinit var m_textMessageIn: TextView
    lateinit var m_editMessageOut: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        m_textMessageIn  = findViewById(R.id.textViewMessageIn)
        m_editMessageOut = findViewById(R.id.editTextMessageOut)

        //..... Set up link to the NetViewModel
        netViewModel = ViewModelProvider(this).get(NetViewModel::class.java)
        netViewModel.netData.observe(this, {
            val sData = it
            m_textMessageIn.text = sData
        })

        netViewModel.socketStatusCode.observe(this, {
            val sCode = it
            //.... If Connect
            if (sCode == SOCKET_CONNECTEDED) {
                //..... Change the Connect Button
                //
                //..... Make the SEND button visible
                //enableButtonSend (true)
                //..... For debugging, show the message behind the IP address after disconnection
                //m_editIP.setText ("")
            }
            //.... If socket is closed
            if (sCode == SOCKET_CLOSED) {
                //..... 2021/03/18: Connection lost message is now handled in vieModel.readMessage
                //      in order to centralize all the messages shown in netData
                //var sData : String = m_textMessages.text.toString()
                //sData = sData + "*** Connection lost ***\n"
                //m_textMessages.setText (sData)
                //m_textMessages.text = sData

                //xxxxx 2021/03/18: Making sendButton invisible causes all the messages to disappear
                //                  ans the Hint for the send text to appear at the top.
                //                  This may be due to the Relative Layout.
                //                  If so, may have to change to Constraint Layout.
                //enableButtonSend (false)
                //.... If socket is closed
            }
            if (sCode == SOCKET_LOST) {
                //..... 2021/03/18: Connection lost message is now handled in vieModel.readMessage
                //      in order to centralize all the messages shown in netData
                //var sData : String = m_textMessages.text.toString()
                //sData = sData + "*** Connection lost ***\n"
                //m_textMessages.setText (sData)
                //m_textMessages.text = sData

                //xxxxx 2021/03/18: Making sendButton invisible causes all the messages to disappear
                //                  ans the Hint for the send text to appear at the top.
                //                  This may be due to the Relative Layout.
                //                  If so, may have to change to Constraint Layout.
                //enableButtonSend (false)
            }

        })

        //..... Get HTTP Session data first before starting NicknameActivity
        //..... Note: The following statement only queues the HTTP request in the background
        //      and the HTTP buffer is not yet obtained at this stage.
        //      However, the HTTP should be available while the User responds during the NicknameActivity.
        netViewModel.getHttpSessionData(this, m_sSessionID)

        //..... Start NicknameActivity to get or confirm the Nickname
        //      And to give time for buildSessionTable
        val intent = Intent(this, NicknameActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_GET_NICKNAME)
   }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //..... Process Nickname if returned from NicknameActivity
        if (requestCode == REQUEST_CODE_GET_NICKNAME) {
            //..... 2021/03/20: The following codes now work after inserting .toString()
            //      to the intent.puExtra () in NicknameActivity.onClickButtonOK as follows:
            //          intent.putExtra (NICKNAME_KEY, sNickname.toString())
            //      This solution was found in Stack Overflow. The URL is below
            //          https://stackoverflow.com/questions/15555750/android-intent-getstringextra-returns-null
            val sNickname = data?.getStringExtra(NICKNAME_KEY)
            if (sNickname.isNullOrEmpty()) {
                //..... We HAVE to get the Nickname again
                val intent = Intent(this, NicknameActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_GET_NICKNAME)
                return
            }
            m_sNickname = sNickname
            //..... Write this information to the device memory
            savePreferenceData(m_sNickname)

            //..... Now start the next process
            processAfterNickname()
            return
        }

        //..... Begin Chat if returned from SessionActivity
        if (requestCode == REQUEST_CODE_GET_SESSION_INFORMATION) {
            //..... Get the Session action and group ID from the button text
            val sStart = getString(R.string.button_text_start)
            val sJoin = getString(R.string.button_text_join)
            val sButtonText = data?.getStringExtra (RETURN_DATA_SESSION_ACTION_KEY)
            if (sButtonText != null) {
                val iIndex = sButtonText.indexOf(" ")
                //..... Get the Action type (Join or Start)
                val sAction = sButtonText.substring(0, iIndex)
                //..... Get the Session Grup ID
                val iSessionID = sButtonText.substring(iIndex + 1).toInt()

                //..... Is this to Start a Chatter session?
                if (sAction == sStart) {


                } else
                if (sAction == sJoin) {
                    processJoin (iSessionID)
                }
            }
            // else do nothing

        }
    }

    /**************************************************************************************
     *      The Process after a Nickname is obtained
     **************************************************************************************/
    fun processAfterNickname () {

        //..... Set the Nickname in the Title Bar
        addNicknameToTitleBar()

        //....................................................................................
        //  Here we have a BIG assumption that netViewModel.m_sHttpBuffer is available after
        //  NicknameActivity. If not, m_sHttpBuffer will be a null string.
        //  In the future, it my be necessary to handle such a case.
        //....................................................................................
        //..... Start SessionActivity to ask if the user wants to Start or Join a Chatter session
        val intent = Intent(this, SessionActivity::class.java)
        intent.putExtra(HTTP_BUFFER, netViewModel.m_sHttpBuffer)
        startActivityForResult(intent, REQUEST_CODE_GET_SESSION_INFORMATION)
        m_sHttpBuffer = netViewModel.m_sHttpBuffer
        return
    }

    /**************************************************************************************
     *      Functions for Nickname
     **************************************************************************************/
    fun getNicknameFromDevice () : String {

        lateinit var sNickname : String

        //..... Get the Nickname from SharedPreference
        ////////////////////////////////////////////////////////////////////////////////////////////
        //      NOTE: The following statement resulted in deprecated indication initially.
        //      To alleviate this issue, take the following steps
        //      (1) In build.gradle (:app), add the following statement
        //          implementation 'androidx.preference:preference-ktx:1.1.1'
        //      (2) Change the import statement at the start of this source program from android.... to androidx....
        //          import androidx.preference.PreferenceManager
        ////////////////////////////////////////////////////////////////////////////////////////////
        PreferenceManager.getDefaultSharedPreferences(this).apply {
            sNickname = getString (NICKNAME_KEY, "").toString()
        }
        return sNickname
    }

    fun savePreferenceData (sData: String) {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = pref.edit()
        editor.putString(NICKNAME_KEY, sData)
            .apply()
    }

    fun addNicknameToTitleBar () {
        val sTitle = "Chatter " + m_sNickname
        setTitle(sTitle)
    }

    /**************************************************************************************
     *      Option Menu processes
     **************************************************************************************/
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menu_edit_nickname -> {
                //Start NicknameActivity to change the nickname
                val intent = Intent(this, NicknameActivity::class.java)
                intent.putExtra (NICKNAME_KEY, m_sNickname)
                startActivityForResult(intent, REQUEST_CODE_GET_NICKNAME)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**************************************************************************************
     *      Socket Communication functions
     **************************************************************************************/
    fun processJoin (iSessionID: Int) {

        //..... Remember iSessionID from the Join Button is one-relative
        val iIndex = iSessionID -1

        //..... 2021/05/09: The netViewModel.m_sessionTable[] that was created by SessionActivity
        //      is gone for some reason and we must rebuild it once again.
        netViewModel.unformatSessionData(m_sHttpBuffer)
        val sessionRecord = netViewModel.m_sessionTable[iIndex]
        val sHostIpAdress = sessionRecord.sessionHostIpAddressLocal
        val iPort = sessionRecord.sessionHostPortNumber

        //..... Connect to Server
        netViewModel.connectToServer(sHostIpAdress, iPort)


    }

    fun onClickButtonSend (view: View) {

        val sMessage = m_editMessageOut.getText().toString()
        //..... Connect to Server using the NetViewModel function
        netViewModel.sendMessage(sMessage)
        //..... Don't forget to erase the editMessage
        m_editMessageOut.setText ("")

    }


}