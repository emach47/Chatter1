package com.example.chatter1

//..... 2021/04/22: the following 2 statements added when viewModelScope.launch {} is coded

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.nio.charset.Charset

const val SESSION_TABLE_STARTED = 0
const val SESSION_TABLE_COMPLETED = 1

class NetViewModel : ViewModel() {

    val httpBuffer = MutableLiveData<String>()

    //--------------------------------------------------------------------------------------------
    //  Data area for Session Infoamation
    //--------------------------------------------------------------------------------------------
    var m_sSessionID = ""
    var m_iSessionTableStatus = 0
    var m_iError = 0
    var m_iSessions = 0                 //Current number of sessions in progress
    var m_iIpAddressClient = Array<Int>(4) {0}
    var m_sessionTable = MutableList(MAX_SESSIONS) {SessionRecord()}
    var m_sHttpBuffer = ""

    fun getHttpSessionData (sSessionID: String) {
        viewModelScope.launch {
            m_sSessionID = sSessionID
            val sURL = HTTP_SESSION_SHOW + sSessionID
            httpBuffer.value = getHttpResponse (sURL)
        }
    }

    suspend fun getHttpResponse (sUrl: String) : String {

        var sResponse = ""

        withContext(Dispatchers.IO) {
            val url = URL(sUrl)
            sResponse = url.readText(Charset.defaultCharset())
        }
        return sResponse
    }

    //..... 2021/04/22: the URL function in the following copy from the Gassner's tutorial also gets a warning message.
    //      I decided to ignore the warning.
    suspend fun getHttpResponse2 (sUrl: String) : String {
        return withContext(Dispatchers.IO) {
            val url = URL(sUrl)
            return@withContext url.readText(Charset.defaultCharset())
        }
    }

//
//    fun getSussionCount () : Int {
//        if (m_iSessions == 0) {
//            return m_iSessions
//        }
//        else {
//            return m_iSessions
//        }
//    }

    fun buildSessionTable (context: AppCompatActivity, sSessionID: String) {

        m_sSessionID = sSessionID

        setSessionStatus(SESSION_TABLE_STARTED)

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(context)
        val sURL = HTTP_SESSION_SHOW + m_sSessionID

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(
            //..... The following statement chaged to remove syntax error per stack overflow
            //      https://stackoverflow.com/questions/32228877/cannot-resolve-symbol-method
            //DownloadManager.Request.Method.GET, sURL,
            Request.Method.GET, sURL,
            { response ->
                // Display the first 500 characters of the response string.
                //textData.text = "Method 1: " + "${response}"
                m_sHttpBuffer = response
                unformatSessionData(m_sHttpBuffer)
                //..... move the unformatted session data to NetViewData
                //moveSessionData()
                setSessionStatus(SESSION_TABLE_COMPLETED)
            },
            { m_sHttpBuffer = sURL + "<-- didn't work!" })

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }


    ///////////////////////////////////////////////////////////////////////////////////////
    //
    //	The HTTP output consists of
    //  (1) Line 1: Control Data Line
    //  (2) Lines foloowing the Control Data Line:
    //      Session information line per session (couls be null if no sessions in progress)
    //  (3) Last Line: contains "--- END ---"
    //  They have the following format
    //
    //  NOTE 1: Each line terminates with a CR (\n)
    //  NOTE 2: The last END line precedes with 2 CRs (\n\n) and terminates with a CR (\n)
    //
    //  ------------------------------------------------------------------------------------
    //	Control Data Line (First Line)
    //	ER:nnnn			if error
    //	or
    //	OK:0000/ii aaabbbcccddd	if successful
    //
    //	where	ii		The total number of games in progress
    //		aaabbbcccddd	The remote address of the requester.
    //
    //  ------------------------------------------------------------------------------------
    //	Session Line(s) (Optional: 2nd & successive lines)
    //  Each line consists of 84 bytes (followed by CR)
    //	nn:mmwwwxxxyyyzzzpppp<Host name 12 char><Guest1 name 12 char> ... rrrssstttuuu
    //
    //	where	nn		The sequential game number
    //		mm		The number of players in this game
    //		wwwxxxyyyzzz	The local address of the HOST
    //		pppp		The port number
    //		rrrssstttuuu	The remote address of the HOST
    //
    ///////////////////////////////////////////////////////////////////////////////////////
    fun unformatSessionData(sBuffer: String) {

        //var sData = ""
        val sIpAddress: String
        //val iBufferSize = sBuffer.length

        //..... Unformat Control Record
        //val sError  = sBuffer.substring (BYTE_BEGIN_OK, BYTE_END_OK)
        m_iError = sBuffer.substring(BYTE_BEGIN_ERROR, BYTE_END_ERROR).toInt()
        if (m_iError != 0) {
            return
        }
        // Get the number of sessions in progress
        m_iSessions = sBuffer.substring(BYTE_BEGIN_SESSIONS, BYTE_END_SESSIONS).toInt()
        //..... Get the Client IP Address
        sIpAddress = sBuffer.substring(BYTE_BEGIN_IP_ADDRESS_CLIENT, BYTE_BEGIN_IP_ADDRESS_CLIENT + BYTE_SIZE_IP_ADDRESS)
        unformatIpAddress(sIpAddress, m_iIpAddressClient)

        //..... Unformat Session Record(s)
        if (m_iSessions > 0) {
            var iSession = 0
            var iStart = BYTE_BEGIN_SESSION_RECORD
            var iEnd = iStart + BYTE_SIZE_SESSION_RECORD
            var sSessionRecord = ""
            var sessionRecord: SessionRecord
            while (iSession < m_iSessions) {
                sSessionRecord = sBuffer.substring(iStart, iEnd)
                sessionRecord = unformatSessionRecord(sSessionRecord)
                m_sessionTable[iSession] = sessionRecord
                iSession++
                iStart = iStart + BYTE_SIZE_SESSION_RECORD
                iEnd = iEnd + BYTE_SIZE_SESSION_RECORD
            }
        }
    }

    fun unformatIpAddress(sIpAddress: String, iIpAddress: Array<Int>) {
        for (i in 0..3) {
            //m_iIpAddressClient[i] = sBuffer.substring (iIpStart, iIpStart+3).toInt()
            val sData = sIpAddress.substring(i * 3, (i + 1) * 3)
            iIpAddress[i] = sData.toInt()
        }
    }

    fun unformatSessionRecord(sSessionRecord: String): SessionRecord {

        val sessionRecord = SessionRecord()

        //..... Set Session ID as one-relative number
        var sData = sSessionRecord.substring(BYTE_BEGIN_SESSION_ID, BYTE_BEGIN_SESSION_ID + BYTE_SIZE_SESSION_ID)
        sessionRecord.sessionGroupId = sData.toInt()
        //..... Get # of Members in this session
        sData = sSessionRecord.substring(BYTE_BEGIN_SESSION_MEMBERS, BYTE_BEGIN_SESSION_MEMBERS + BYTE_SIZE_SESSION_ID)
        val iMembers = sData.toInt()
        sessionRecord.sessionMembers = iMembers
        //..... Get the Host local IP address
        sData = sSessionRecord.substring(BYTE_BEGIN_SESSION_HOST_IP_ADDRESS_LOCAL, BYTE_BEGIN_SESSION_HOST_PORT_NUMBER)
        unformatIpAddress(sData, sessionRecord.sessionHostIpAddressLocal)
        //..... Get the Port Number
        sData = sSessionRecord.substring(BYTE_BEGIN_SESSION_HOST_PORT_NUMBER, BYTE_BEGIN_SESSION_HOST_NAME)
        sessionRecord.sessionHostPortNumber = sData.toInt()
        //..... Get Host name for this session
        sessionRecord.sessionHostName = sSessionRecord.substring(BYTE_BEGIN_SESSION_HOST_NAME, BYTE_BEGIN_SESSION_GUEST_NAME)
        //..... Get the Guest Names if any
        if (iMembers > 1) {
            var i = 0
            var iStart = BYTE_BEGIN_SESSION_GUEST_NAME
            while (i < iMembers) {
                sessionRecord.sessionGuestName[i] = sSessionRecord.substring(iStart, iStart + NICKNAME_MAX_SIZE)
                i++
                iStart = iStart + NICKNAME_MAX_SIZE
            }
        }
        //..... Get Host local IP address
        sData = sSessionRecord.substring(BYTE_BEGIN_SESSION_HOST_IP_ADDRESS_EXTERNAL, BYTE_BEGIN_SESSION_HOST_IP_ADDRESS_EXTERNAL + BYTE_SIZE_IP_ADDRESS)
        unformatIpAddress(sData, sessionRecord.sessionHostIpAddressExternal)

        return sessionRecord
    }


    fun setSessionStatus (iStatus: Int) {
        m_iSessionTableStatus = iStatus
    }

    fun moveSessionData() {

        //..... Since netViewModel has never been initialized, do it here
        //netViewModel = NetViewModel()

        m_iError = m_iError
        m_iSessions = m_iSessions
        var i = 0
        while (i < m_iSessions) {
            m_sessionTable[i] = m_sessionTable[i]
            i++
        }
    }

//
//    fun setSessionID (sSessionID: String) {
//        m_sSessionID = sSessionID
//    }
//
//    fun getHttpData(iRequestCode : Int) {
//        viewModelScope.launch {
//            httpBuffer.value = getHttpBuffer(iRequestCode)
//        }
//    }
//    suspend fun getHttpBuffer(iRequestCode : Int): String {
//        //var httpData: String
//
//        val sUrl = formatUrl(iRequestCode)
//
//        return withContext(Dispatchers.IO) {
//            val url = URL(sUrl)
//            return@withContext url.readText(Charset.defaultCharset())
//        }
//    }
//
//    fun formatUrl (iRequestCode: Int) :String {
//
//        var sUrl = ""
//        if (iRequestCode == HTTP_REQUEST_SESSION_INFORMARION) {
//            sUrl = "http://www.machida.com/cgi-bin/show2.pl?Game=" + m_sSessionID
//        }
//
//        return sUrl
//    }
//
//    fun getHttpData(sSessionID: String) {
//        viewModelScope.launch {
//            httpBuffer.value = getHttpBuffer(sSessionID)
//        }
//    }
//
//    suspend fun getHttpBuffer(sSessionID : String): String {
//        var httpData: String
//
//        val sGameID = "Boggle"
//        var sURL = "http://www.machida.com/cgi-bin/show2.pl?Game="
//        sURL = sURL + sGameID
//
//        return withContext(Dispatchers.IO) {
//            var url = URL(sURL)
//            return@withContext url.readText(Charset.defaultCharset())
//
//        }
//
//    }
//
//    fun requestSessionInformation (iRequestCode: Int) {
//
//    }
}