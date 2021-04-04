package com.example.chatter1

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
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

    //..... Define Session ID
    val m_sSessionID = "RumNet"

    //..... Properties for NicknameActivity
    lateinit var m_sNickname : String

    //=============================================================================================
    //  Properties for NetLogin
    //=============================================================================================
    lateinit var netLogin : NetLogin
    //..... returned HTTP Buffer
    lateinit var m_sHttpBuffer : String
    var m_iHttpRequestCode = HTTP_REQUEST_SESSION_INFORMARION

    //..... Properties for NetViewModel
    lateinit var netViewModel : NetViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //..... Instantiate NetLogin
        netLogin = NetLogin()
        netLogin.initNetLogin (m_sSessionID)

        /**************************************************************************************
         *      Get Nickname
         **************************************************************************************/
        //..... Get m_sNickname from the device
        m_sNickname = getNicknameFromDevice()
        //..... If the Nickname does not exist yet, we must ask the user to create one
        if (m_sNickname.isEmpty()) {
            val intent = Intent(this, NicknameActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_GET_NICKNAME)
            //..... Wait until NicknameActivity finishes before starting the next step
            //      That is, the next step is called from onActivityResult(
            return
        }

        //..... We have the Nickname; Start the next process
        processAfterNickname()
   }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_GET_NICKNAME) {
            //..... 2021/03/20: The following codes now work after inserting .toString()
            //      to the intent.puExtra () in NicknameActivity.onClickButtonOK as follows:
            //          intent.putExtra (NICKNAME_KEY, sNickname.toString())
            //      This solution was found in Stack Overflow. The URL is below
            //          https://stackoverflow.com/questions/15555750/android-intent-getstringextra-returns-null
            val sNickname = data?.getStringExtra(NICKNAME_KEY)
            if (sNickname.isNullOrEmpty()) {
                //..... WE HAVE to get the Nickname again
                val intent = Intent(this, NicknameActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_GET_NICKNAME)
                return
            }
            //..... We have a stored Nickname, let's use it
            m_sNickname = sNickname
            //..... Write this information to the device memory
            savePreferenceData(m_sNickname)

            //..... Now start the next process
            processAfterNickname()
            return
        }
        if (requestCode == REQUEST_CODE_GET_SESSION_INFORMATION) {

        }
    }

    /**************************************************************************************
     *      The Process after a Nickname is obtained
     **************************************************************************************/
    fun processAfterNickname () {

        //..... Set the Nickname in the Title Bar
        addNicknameToTitleBar()

        /**************************************************************************************
         *      Get Chatter session info from Internet
         **************************************************************************************/
        val netLogin = NetLogin()
        netLogin.getSessionInfo2(this, m_sSessionID)


        val intent = Intent(this, SessionActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_GET_SESSION_INFORMATION)
        return

        //===== The following activities are now performed by SessionActivity; We need wait until it finishes
//        //..... Start SessionActivity to get the session information from Internet
//        val textViewMessage : TextView = findViewById(R.id.textViewMessage)
//        //..... Instantiate netViewModel
//        netViewModel = ViewModelProvider(this).get(NetViewModel::class.java)
//        //..... Setup Session ID
//        netViewModel.setSessionID(m_sSessionID)
//        //..... Setup observer for NetViewModel to this Activity
//        netViewModel.httpBuffer.observe(this, Observer {
//            //..... We have the response from the HTTP operation
//            textViewMessage.text = it
//            m_sHttpBuffer = it
//            processHttpData()
//        })
//
//        //..... Show all the sessions in progress
//        requestSessionInformation()
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
//
//    fun requestSessionInformation () {
//
//        //..... Reuest the data showing all sessions in progress
//        //      Note: the result will be processed by ?????
//        m_iHttpRequestCode = HTTP_REQUEST_SESSION_INFORMARION
//        netViewModel.getHttpData(m_iHttpRequestCode)
//        //..... Upon completion processHttpData() will be called
//    }
//
//    fun processHttpData () {
//
//        netLogin.processHttpData(m_iHttpRequestCode, m_sHttpBuffer)
//
//    }
//
//    fun internetGetData(sGameID: String) {
//
//        // Instantiate the RequestQueue.
//        //val myContext = getApplicationContext()
//        val queue = Volley.newRequestQueue(this)
//        var sURL = "http://www.machida.com/cgi-bin/show2.pl?Game="
//        sURL = sURL + sGameID
//        //var sData = ""
//        //val textData = findViewById<TextView>(R.id.textData)
//
//        // Request a string response from the provided URL.
//        val stringRequest = StringRequest(
//            //..... The following statement chaged to remove syntax error per stack overflow
//            //      https://stackoverflow.com/questions/32228877/cannot-resolve-symbol-method
//            //DownloadManager.Request.Method.GET, sURL,
//            Request.Method.GET, sURL,
//            Response.Listener<String> { response ->
//                // Display the first 500 characters of the response string.
//                //textData.text = "Method 1: " + "${response}"
//                val sData = "${response}"
//                //copyToTextView(sData)
//            },
//            Response.ErrorListener { sURL = sURL + "<-- didn't work!" })
//
//        // Add the request to the RequestQueue.
//        queue.add(stringRequest)
//    }

    /**************************************************************************************
     *      Process after SessionActivity
     **************************************************************************************/
    fun processAfterSessionActivity () {

        //TODO Code after Session Activity

    }


}