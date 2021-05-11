package com.example.chatter1

//..... 2021/04/22: the following 2 statements added when viewModelScope.launch {} is coded

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread

const val SESSION_TABLE_STARTED = 0
const val SESSION_TABLE_COMPLETED = 1

const val MESSAGE_KEY = "message_data"
const val MESSAGE_KEY_IN = "message_in"
const val MESSAGE_KEY_OUT = "message_out"
//.....
const val SOCKET_STATUS_KEY = "socket_status"
const val SOCKET_CLOSED = "Closed"
const val SOCKET_CONNECTEDED = "Connected"
const val SOCKET_LOST = "Lost"

class NetViewModel : ViewModel() {

    //==============================================================================================
    //  liveData for network
    //==============================================================================================
    //..... Message
    val netData = MutableLiveData<String>()

    //val httpBuffer = MutableLiveData<String>()

    //..... Socket connect and disconnect
    val socketStatusCode = MutableLiveData<String>()

    //==============================================================================================
    //  Data for Socket
    //==============================================================================================
    lateinit var m_socket: Socket

    //..... 2021/02/28: Added input/output for socket connection
    lateinit var input: BufferedReader
    lateinit var output: PrintWriter


    //..... The switch to loop whether to read socket or not
    var m_bOkToTryReadSocket = true

    //--------------------------------------------------------------------------------------------
    //  Data area for Session Infoamation
    //--------------------------------------------------------------------------------------------
    var m_sSessionID = ""
    var m_iSessionTableStatus = 0
    var m_iError = 0
    var m_iSessions = 0                 //Current number of sessions in progress
    //var m_iIpAddressClient = Array<Int>(4) {0}
    var m_sIpAddressClient = ""
    //..... 2021/05/06: Changed due to use expandable List
    //var m_sessionTable = MutableList(MAX_SESSIONS) {SessionRecord()}
    var m_sessionTable : MutableList<SessionRecord> = mutableListOf<SessionRecord>()

    var m_sHttpBuffer = ""

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val bundle = msg.data
            val message = bundle?.getString(MESSAGE_KEY)
            if (message != null) {
                //netData.value = netData.value + message + "\n"
                appendToNetData(message)
            }
            val sConnect = bundle.getString (SOCKET_STATUS_KEY)
            if (sConnect != null) {
                if (sConnect == SOCKET_CONNECTEDED) {
                    //..... Perform the Socket connected process
                    socketStatusCode.value = sConnect
                } else if (sConnect == SOCKET_LOST) {
                    //..... Display the connection is lost
                    appendToNetData("*** Connection Lost ***")
                    //..... Perform the Socket closed process
                    socketStatusCode.value = sConnect
                } else if (sConnect == SOCKET_CLOSED) {
                    //..... Display the connection is lost
                    appendToNetData("=== Socket is closed ===")
                    //..... Perform the Socket closed process
                    socketStatusCode.value = sConnect
                }
            }
        }
    }



//
//    fun getHttpSessionData (sSessionID: String) {
//        viewModelScope.launch {
//            m_sSessionID = sSessionID
//            val sURL = HTTP_SESSION_SHOW + sSessionID
//            httpBuffer.value = getHttpResponse (sURL)
//        }
//    }
//
//    suspend fun getHttpResponse (sUrl: String) : String {
//
//        var sResponse = ""
//
//        withContext(Dispatchers.IO) {
//            val url = URL(sUrl)
//            sResponse = url.readText(Charset.defaultCharset())
//        }
//        return sResponse
//    }
//
//    //..... 2021/04/22: the URL function in the following copy from the Gassner's tutorial also gets a warning message.
//    //      I decided to ignore the warning.
//    suspend fun getHttpResponse2 (sUrl: String) : String {
//        return withContext(Dispatchers.IO) {
//            val url = URL(sUrl)
//            return@withContext url.readText(Charset.defaultCharset())
//        }
//    }
//
//
//    fun getSussionCount () : Int {
//        if (m_iSessions == 0) {
//            return m_iSessions
//        }
//        else {
//            return m_iSessions
//        }
//    }

    fun getHttpSessionData (context: AppCompatActivity, sSessionID: String) {

        m_sSessionID = sSessionID

        setSessionStatus(SESSION_TABLE_STARTED)

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(context)
        val sURL = HTTP_SESSION_SHOW + m_sSessionID

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(
                //..... The following statement changed to remove syntax error per stack overflow
                //      https://stackoverflow.com/questions/32228877/cannot-resolve-symbol-method
                //DownloadManager.Request.Method.GET, sURL,
                Request.Method.GET, sURL,
                { response ->
                    m_sHttpBuffer = response
                    //unformatSessionData(m_sHttpBuffer)
                    //..... move the unformatted session data to NetViewData
                    //moveSessionData()
                    setSessionStatus(SESSION_TABLE_COMPLETED)
                },
                { m_sHttpBuffer = sURL + "<-- didn't work!" }
        )
        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }
//
//    fun buildSessionTable (context: AppCompatActivity, sSessionID: String) {
//
//        m_sSessionID = sSessionID
//
//        setSessionStatus(SESSION_TABLE_STARTED)
//
//        // Instantiate the RequestQueue.
//        val queue = Volley.newRequestQueue(context)
//        val sURL = HTTP_SESSION_SHOW + m_sSessionID
//
//        // Request a string response from the provided URL.
//        val stringRequest = StringRequest(
//            //..... The following statement changed to remove syntax error per stack overflow
//            //      https://stackoverflow.com/questions/32228877/cannot-resolve-symbol-method
//            //DownloadManager.Request.Method.GET, sURL,
//            Request.Method.GET, sURL,
//            { response ->
//                m_sHttpBuffer = response
//                unformatSessionData(m_sHttpBuffer)
//                //..... move the unformatted session data to NetViewData
//                //moveSessionData()
//                setSessionStatus(SESSION_TABLE_COMPLETED)
//            },
//            { m_sHttpBuffer = sURL + "<-- didn't work!" })
//
//        // Add the request to the RequestQueue.
//        queue.add(stringRequest)
//    }


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
        var sessionRecord: SessionRecord

        //..... Unformat Control Record
        //val sError  = sBuffer.substring (BYTE_BEGIN_OK, BYTE_END_OK)
        m_iError = sBuffer.substring(BYTE_BEGIN_ERROR, BYTE_END_ERROR).toInt()
        if (m_iError != 0) {
            m_iSessions = 0
            return
        }
        // Get the number of sessions in progress
        m_iSessions = sBuffer.substring(BYTE_BEGIN_SESSIONS, BYTE_END_SESSIONS).toInt()
        //..... Get the Client IP Address
        sIpAddress = sBuffer.substring(BYTE_BEGIN_IP_ADDRESS_CLIENT, BYTE_BEGIN_IP_ADDRESS_CLIENT + BYTE_SIZE_IP_ADDRESS)
        m_sIpAddressClient = unformatIpAddress(sIpAddress)

        //..... Unformat Session Record(s)
        if (m_iSessions > 0) {
            var iSession = 0
            var iStart = BYTE_BEGIN_SESSION_RECORD
            var iEnd = iStart + BYTE_SIZE_SESSION_RECORD
            var sSessionRecord = ""
            while (iSession < m_iSessions) {
                sSessionRecord = sBuffer.substring(iStart, iEnd)
                sessionRecord = unformatSessionRecord(sSessionRecord)
                //..... 2021/05/06: Changed to make m_sessionTable[] expandble list
                //m_sessionTable[iSession] = sessionRecord
                m_sessionTable.add(sessionRecord)
                iSession++
                iStart = iStart + BYTE_SIZE_SESSION_RECORD
                iEnd = iEnd + BYTE_SIZE_SESSION_RECORD
            }
        }
        //..... 2021/05/06: Ass a blank sessionRecord at the end
        sessionRecord = SessionRecord()
        m_sessionTable.add(sessionRecord)
    }
//
//    fun unformatIpAddress(sIpAddress: String, iIpAddress: Array<Int>) {
//        for (i in 0..3) {
//            //m_iIpAddressClient[i] = sBuffer.substring (iIpStart, iIpStart+3).toInt()
//            val sData = sIpAddress.substring(i * 3, (i + 1) * 3)
//            iIpAddress[i] = sData.toInt()
//        }
//    }

    fun unformatIpAddress(sString: String) : String {

        var sIpAddress = ""
        for (i in 0..3) {
            //m_iIpAddressClient[i] = sBuffer.substring (iIpStart, iIpStart+3).toInt()
            val sData = sString.substring(i * 3, (i + 1) * 3)
            val iData = sData.toInt()
            val sIp = iData.toString()
            if (sIpAddress.isNotEmpty()) {
                sIpAddress= sIpAddress + "."
            }
            sIpAddress= sIpAddress + sIp
        }
        return sIpAddress
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
        //unformatIpAddress(sData, sessionRecord.sessionHostIpAddressLocal)
        sessionRecord.sessionHostIpAddressLocal = unformatIpAddress(sData)
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
        //unformatIpAddress(sData, sessionRecord.sessionHostIpAddressExternal)
        sessionRecord.sessionHostIpAddressExternal = unformatIpAddress(sData)

        return sessionRecord
    }


    fun setSessionStatus (iStatus: Int) {
        m_iSessionTableStatus = iStatus
    }
//
//    fun moveSessionData() {
//
//        //..... Since netViewModel has never been initialized, do it here
//        //netViewModel = NetViewModel()
//
//        m_iError = m_iError
//        m_iSessions = m_iSessions
//        var i = 0
//        while (i < m_iSessions) {
//            m_sessionTable[i] = m_sessionTable[i]
//            i++
//        }
//    }

    //==================================================================================================================
    //      Socket operations
    //==================================================================================================================

    fun connectToServer(sServerIpAddress: String, iServerPort: Int) {
        viewModelScope.launch {
            val sMessage = getSocketData(sServerIpAddress, iServerPort)
            //..... 2021/03/18: The following statement will clear all the previous messages if present.
            //                  Consider if this is OK or preserve the previous messages.
            //netData.value = sResult
            appendToNetData(sMessage)
        }
    }

    suspend fun getSocketData(sServerIpAddress: String, iServerPort: Int): String {
        var sData: String

        sData = withContext(Dispatchers.IO) {
            //..... NOTE: THe following statement will hang forever if the Server is not running.
            //          A new process to handle such case should be implemented.
            //          For example, pass "Connecting ..." to UI via Bundle to notify the user
            //          and erase with the "Connected ... " message with the return.
            m_socket = Socket(sServerIpAddress, iServerPort)
            output = PrintWriter(m_socket.getOutputStream())
            input = BufferedReader(InputStreamReader(m_socket.getInputStream()))
            sData = "Connected to " + sServerIpAddress + " (" + iServerPort.toString() + ")"
            putBundleString(SOCKET_STATUS_KEY, SOCKET_CONNECTEDED)
            //..... Start a forever loop to get incoming messages from the Server
            m_bOkToTryReadSocket = true
            readMessage ()
            //xxxxx The following return of the socket connection may be useless.
            //..... Return the result of the socket connection
            return@withContext sData
        }
        return sData
    }

    fun putBundleString (sMessageKey : String, sMessage: String) {

        val bundle = Bundle()
        bundle.putString(sMessageKey, sMessage)
        Message().also {
            it.data = bundle
            handler.sendMessage(it)
        }

    }

    fun appendToNetData (sMessage : String) {
        var sData = netData.value
        //..... 2021/03/18: nData.value returns "null" if it is null. Remove it.
        if (sData.isNullOrEmpty()) {
            sData = ""
        }
        sData = sData + sMessage + "\n"
        netData.value = sData
    }

    ////////////////////////////////////////////////////////////////////////////
    //      readMessage () reads a message sent by Server.
    //
    //      This is a forever-looping process and constantly checks for an incoming
    //      message from the Server.
    //      When a message is received via input.read(), it puts it in the bundle
    //      so that the UI thread (MainActivity) can use it to display in the TextView.
    //
    ////////////////////////////////////////////////////////////////////////////
    private fun readMessage() {
        thread(start = true) {
            val bundle = Bundle()
            while (m_bOkToTryReadSocket) {
                try {
                    var message: String = input.readLine()
                    message = "Server: " + message
                    //..... The following statement will not compile
                    //runOnUiThread(Runnable { tvMessages.append("server: $message\n") })
                    //..... The following statement causes a runtime failure
                    //netData.value = netData.value + message + "\n"
                    bundle.putString(MESSAGE_KEY, message)
                    Message().also {
                        it.data = bundle
                        handler.sendMessage(it)
                    }
                } catch (e: Exception) {
                    //..... Is this exception the result of the DISCONNECT Button?
                    if (m_bOkToTryReadSocket) {
                        //..... No, the connection to the server is lost
                        putBundleString(SOCKET_STATUS_KEY, SOCKET_LOST)
                        closeSocket (m_socket)
                        e.printStackTrace()
                        //..... Stop the readLine() loop
                        m_bOkToTryReadSocket = false
                    }
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    //      sendMessage (sMessage) sends a message to Server.
    //
    //      This function is called from MainAvtivity when the user press the SEND button.
    //      It passes the sent message back to the UI thread via bundle
    //      so that the user can recognize the message was sent.
    //
    ////////////////////////////////////////////////////////////////////////////
    fun sendMessage(sMessage: String) {

        thread(start = true) {

//            val bundle = Bundle()
            //..... Send the message
            output.write(sMessage)
            //..... 2021/03/08: Must put a CR to complete sending the message
            output.println()
            output.flush()

            //..... Send sMessage to UI via bundle
//            bundle.putString(MESSAGE_KEY, sMessage)
//            Message().also {
//                it.data = bundle
//                handler.sendMessage(it)
//            }
            putBundleString (MESSAGE_KEY, sMessage)
        }
    }

    fun closeSocket (socket: Socket) {

        m_bOkToTryReadSocket = false
        socket.close()
        //..... Notify MainActivity
        putBundleString(SOCKET_STATUS_KEY, SOCKET_CLOSED)

    }

}