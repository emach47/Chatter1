package com.example.chatter1

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import java.lang.Thread.sleep

const val NICKNAME_KEY = "Nickname"
//!!!!! 2021/03/28: NOTE: Nickname policy has a potential problem.
//      The User can change Nickname any time by selecting the Change Nickname menu option.
//      However, this app assumes that Nickname is confirmed or changed at the start of the app.
//      Is it OK to change the Nickname anytime, even if during the Chatter session?
const val REQUEST_CODE_GET_NICKNAME = 1234
const val REQUEST_CODE_GET_SESSION_INFORMATION = 200

//..... HTTP operation request code
const val HTTP_REQUEST_SESSION_INFORMATION    = 1
const val HTTP_REQUEST_ADD_HOST               = 2
const val HTTP_REQUEST_REMOVE_HOST            = 3
const val HTTP_REQUEST_ADD_GUEST              = 4
const val HTTP_REQUEST_REMOVE_GUEST           = 5

const val SOCKET_MODE_GUEST           = 0
const val SOCKET_MODE_HOST            = 1


/*********************************************************************************************
 * V1.01    2021/06/14
 *      This is the first version of workable Chatter1.
 *      Chatter1 provides a chat service using socket connection.
 *      Any one can become a Host of chat session or join a chat session hosted by a Host
 *      The management of multiple chat sessions is done through machida.com bulletin board.
 *
 *      Complex Program Process Sequence
 *      This first version has a complex process sequence in order to "synchronize" HTTP data
 *      exchange. I wanted to have a "synchronous" HTTP operation but Android requires a background
 *      process to access HTTP. My attempts to use Volley and Coroutines are not successful
 *      because of my lack of understanding of these techniques. The final result was a complex
 *      process sequence that is difficult to follow. It is my hope that this will be resolved
 *      in the future upgrades.
*********************************************************************************************/
class MainActivity : AppCompatActivity() {

    var m_iSocketMode = SOCKET_MODE_GUEST

    //..... Define Session ID
    val m_sGameID = "RumNet"

    //..... Properties for NicknameActivity
    var m_sNickname =""

    //=============================================================================================
    //  Properties for NetLogin
    //=============================================================================================
    //..... returned HTTP Buffer
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

        netViewModel.httpResponse.observe(this, {
            val sHttpBuffer = it
            //..... Save sHttpBuffer in m_sHttpBuffer
            m_sHttpBuffer = sHttpBuffer
            //..... Start Session Activity
            startSessionActivity(sHttpBuffer)
        })

        netViewModel.httpResponseRemoveHost.observe(this, {
            val sHttpBuffer = it
            var sMessage = "remHost "
            val sOK = sHttpBuffer.substring(0,2)
            if (sOK == "OK") {
                sMessage = sMessage + "worked"
            } else {
                sMessage = sMessage + "FAILED *********"
            }
            Toast.makeText(this, sMessage, Toast.LENGTH_SHORT).show()

            netViewModel.httpGetSessionData(this, m_sGameID)
        })

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
            val sQuit = getString(R.string.button_text_quit)
            val sButtonText = data?.getStringExtra (RETURN_DATA_SESSION_ACTION_KEY)
            //..... If the Quit button is clicked, end the app now
            if (sButtonText == sQuit) {
                //..... Need both of the following statements to shut down without any error
                finishAffinity()
                System.exit (0)
            }
            if (sButtonText != null) {
                val iIndex = sButtonText.indexOf(" ")
                //..... Get the Action type (Join or Start)
                val sAction = sButtonText.substring(0, iIndex)
                //..... Get the Session Grup ID
                val iSessionID = sButtonText.substring(iIndex + 1).toInt()
                //..... Is this to Start a Chatter session?
                if (sAction == sStart) {
                    m_iSocketMode = SOCKET_MODE_HOST
                    //processStartSession(iSessionID)
                    processStartSession()
                } else
                if (sAction == sJoin) {
                    m_iSocketMode = SOCKET_MODE_GUEST
                    processJoinSession (iSessionID)
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
        //..... Get the Session Data via HTTP and when the data is returned,
        //      SessionActivity will start from onActivityResult()
        //startSessionActivity()
        //..... 2021/06/12: Must pass MainActivity pointer because the Volley HTTP operations
        //                  needs it. Otherwise it will lead to a runtime error.
        netViewModel.httpGetSessionData(this, m_sGameID)

        return
    }

    fun startSessionActivity () {
        //....................................................................................
        //  Here we have a BIG assumption that netViewModel.m_sHttpBuffer is available after
        //  NicknameActivity. If not, m_sHttpBuffer will be a null string.
        //  In the future, it my be necessary to handle such a case.
        //....................................................................................
        //..... Start SessionActivity to ask if the user wants to Start or Join a Chatter session
        m_sHttpBuffer = netViewModel.m_sHttpBuffer
        val intent = Intent(this, SessionActivity::class.java)
        intent.putExtra(HTTP_BUFFER, m_sHttpBuffer)
        startActivityForResult(intent, REQUEST_CODE_GET_SESSION_INFORMATION)

    }

    //..... This startSessionActivity is called after the User select End Session Menu Option
    fun startSessionActivity (sHttpBuffer: String) {
        //....................................................................................
        //  Here we have a BIG assumption that netViewModel.m_sHttpBuffer is available after
        //  NicknameActivity. If not, m_sHttpBuffer will be a null string.
        //  In the future, it my be necessary to handle such a case.
        //....................................................................................
        //..... Start SessionActivity to ask if the user wants to Start or Join a Chatter session
        val intent = Intent(this, SessionActivity::class.java)
        intent.putExtra(HTTP_BUFFER, sHttpBuffer)
        startActivityForResult(intent, REQUEST_CODE_GET_SESSION_INFORMATION)

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
            R.id.menu_end_session -> {
                //..... Shutdown Chatter Session
                if (m_iSocketMode == SOCKET_MODE_HOST) {
                    //..... Any Guest still connected?
                    //val iGuests = netViewModel.countGuests()
//                    if (iGuests > 0) {
//                    //    ..... Confirm if it is OK to shut down this session
//                    //    ..... Start the confirmation activity
//                    }
                    // else {
                        netViewModel.shutdownHost(this, m_sGameID, m_sNickname)
                    //}
                }
                else //..... SOCKET_MODE_GUEST assumed
                {
                    netViewModel.shutdownConnectionToHost (m_sNickname)
                    //..... Wait 1 sec to give Host the time to remove this Guest
                    sleep (1000)
                    //..... Start SessionActivity
                    netViewModel.httpGetSessionData(this, m_sGameID)
                }

                //..... 2021/06/13: startSessionActivity() is now called from netViewModel.httpResponse.observe
                //startSessionActivity()

            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**************************************************************************************
     *      Socket Communication functions
     **************************************************************************************/
    //fun processStartSession (iSessionID: Int) {
    fun processStartSession () {

        //..... Remember iSessionID from the Join Button is one-relative
        //val iIndex = iSessionID - 1

        // (1)  Do URL=http://www.machida.com/cgi-bin/addhost2.pl?Game=RumNet+HOST=HostName****+ADDR=aaabbbcccddd+PORT=8080
        // (2)  Begin socket listen
        // (3)  When a connection is made, add the client to the socket table up to MAX_MEMBERS
        // (4)  Get guest's NickName and add it to the bulletin board
        //      http://www.machida.com/cgi-bin/addguest2.pl?Game=RumNet+HOST=<Host name>+GUEST=<<GuestName>
        //
        netViewModel.addHost (this, m_sGameID, m_sNickname)
        //..... Start as a Server
        m_textMessageIn.text = "... Host mode started; Waiting guests to join ..."
        netViewModel.startServerThread (m_sNickname)

    }

    fun processJoinSession (iSessionID: Int) {

        //..... Remember iSessionID from the Join Button is one-relative
        val iIndex = iSessionID - 1

        //..... 2021/05/09: The netViewModel.m_sessionTable[] that was created by SessionActivity
        //      is gone for some reason and we must rebuild it once again.
        netViewModel.unformatSessionData(m_sHttpBuffer)
//        val sessionRecord = netViewModel.m_sessionTable[iIndex]
//        val sHostIpAdress = sessionRecord.sessionHostIpAddressLocal
//        val iPort = sessionRecord.sessionHostPortNumber

        //..... Connect to Server
        //netViewModel.connectToServer(sHostIpAdress, iPort)
        netViewModel.connectToServer(iIndex, m_sNickname)

    }

    fun onClickButtonSend (view: View) {

        var sMessage = m_sNickname + ": "
        sMessage = sMessage + m_editMessageOut.getText().toString()
        //..... Use different sendMessage depending on whether Host or Guest
        if (m_iSocketMode == SOCKET_MODE_HOST) {
            netViewModel.broadcastMessage(sMessage, MESSAGE_HOST_ORIGINATED)
        } else {
            netViewModel.sendMessage(sMessage)
        }
        //..... Don't forget to erase the editMessage
        m_editMessageOut.setText ("")

    }

}