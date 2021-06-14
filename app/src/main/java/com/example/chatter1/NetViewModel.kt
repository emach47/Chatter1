package com.example.chatter1

import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
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
import java.io.*
import java.net.InetAddress
import java.net.NetworkInterface.*
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import kotlin.concurrent.thread

const val SESSION_TABLE_STARTED = 0
const val SESSION_TABLE_COMPLETED = 1

const val MESSAGE_KEY = "message_data"
const val MESSAGE_KEY_IN = "message_in"
const val MESSAGE_KEY_OUT = "message_out"

//..... In broadcastMessage
const val MESSAGE_HOST_ORIGINATED = 999
//const val MESSAGE_BROADCAST = 100
//const val MESSAGE_ORIGINAL = 0

//..... Constants for SOCKET operation
const val SOCKET_PORT = 8080

const val SOCKET_STATUS_KEY = "socket_status"
const val SOCKET_CLOSED = "Closed"
const val SOCKET_CONNECTED = "Connected"
const val SOCKET_LOST = "Lost"

//..... MAX_GUESTS is one less than MAX_MEMBERS excluding the Host
const val MAX_GUESTS = MAX_MEMBERS - 1
//..... For Testing Guest Full
//const val MAX_GUESTS = 1

const val GUEST_NAME_EMPTY = "empty"
const val GUEST_NAME_OK = 777

////..... MAX_GUEST_SOCKETS is one larger than MAX_GUESTS to handle a temporary connection
//const val MAX_GUEST_SOCKETS = MAX_GUESTS + 1
const val LOGIN_HELLO = "@HELLO"
//const val LOGIN_RESULT_OK = 1
const val LOGIN_RESULT_REGULAR_MESSAGE = 0
const val LOGIN_RESULT_ERROR = -1
const val LOGOFF_BYE = "@BYE"


class NetViewModel : ViewModel() {

    //==============================================================================================
    //  liveData for network
    //==============================================================================================
    //..... Message
    val netData = MutableLiveData<String>()
    //..... HTTP return data
    val httpResponse = MutableLiveData<String>()
    val httpResponseRemoveHost = MutableLiveData<String>()

//    //..... Socket connect and disconnect
//    val socketStatusCode = MutableLiveData<String>()

    //==============================================================================================
    //  Data used by for Host Mode
    //==============================================================================================
    //..... m_serverSocket is not used & to be deleted
    lateinit var m_serverSocket: ServerSocket
    var m_bOKToAcceptServerSocket = false
    var m_iGuestSockets = 0
    var m_guestSocket = MutableList(MAX_GUESTS) { Socket() }
    var m_guestInput:  Array<BufferedReader?> = Array(MAX_GUESTS) { null }
    var m_guestOutput: Array<PrintWriter?> = Array(MAX_GUESTS) { null }
    var m_bOKToReadGuestSocket = Array(MAX_GUESTS) {false}
    var m_bGuestSocketAccepted = Array(MAX_GUESTS) {false}
    //..... m_sGuestName needs to be replaced with the correct Nickname upon connection
    //var m_sGuestName = arrayOf ("empty", "empty", "empty")
    var m_sGuestName = Array(MAX_GUESTS) { GUEST_NAME_EMPTY }
    var m_sGuestIpAddress = Array(MAX_GUESTS) { "" }


    //--------------------------------------------------------------------------------------------
    //  Data used for Guest Mode
    //--------------------------------------------------------------------------------------------
    //..... m_socket for the Guest mode
    //      It is used (only once) to close the socket in readMessage() when the socket to the Host is closed
    lateinit var m_socket: Socket

    //..... 2021/02/28: Added input/output for Guest socket connection
    lateinit var input: BufferedReader
    lateinit var output: PrintWriter

    //..... The switch to stop the forever loop in readMessage() if it it is closed
    var m_bOkToTryReadSocket = true

    //--------------------------------------------------------------------------------------------
    //  Data area for Session Information
    //--------------------------------------------------------------------------------------------
    var m_sGameID = ""
    var m_iSessionTableStatus = 0
    var m_iError = 0
    var m_iSessions = 0                 //Current number of sessions in progress

    var m_sIpAddressClient = ""
    //..... m_sLocalIpAddress is set in constructUrlAddHost(...) by addHost(...)
    var m_sLocalIpAddress = ""

    //..... 2021/05/06: Changed due to use expandable List
    var m_sessionTable: MutableList<SessionRecord> = mutableListOf<SessionRecord>()
    var m_sHostName = ""
//    var m_sHostIpAddress = ""

    var m_sHttpBuffer = ""
    lateinit var m_context : AppCompatActivity

    //==============================================================================================
    //  handler object to receive message from readMessage(), sendMessage(),
    //  readGuestMessage(iGuest), sendGuestMessage(iGuest, sMessage)
    //==============================================================================================
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val bundle = msg.data
            val message = bundle?.getString(MESSAGE_KEY)
            if (message != null) {
                //netData.value = netData.value + message + "\n"
                appendToNetData(message)
            }
            val sConnect = bundle.getString(SOCKET_STATUS_KEY)
            if (sConnect != null) {
                val sCode = sConnect.take(4)
                if (sCode == SOCKET_CONNECTED.take(4)) {
//                    //..... Perform the Socket connected process
//                    socketStatusCode.value = sConnect
                } else if (sCode == SOCKET_LOST.take(4)) {
                    //..... Display the connection is lost
                    val sMessage =  "*** Connection " + sConnect
                    appendToNetData (sMessage)
//                    //..... Perform the Socket closed process
//                    socketStatusCode.value = sConnect
                } else if (sCode == SOCKET_CLOSED.take(4)) {
                    //..... Display the connection is lost
                    val sMessage = "=== Socket is " + sConnect
                    //appendToNetData("=== Socket is closed ===")
                    appendToNetData(sMessage)
//                    //..... Perform the Socket closed process
//                    socketStatusCode.value = sConnect
                } else if (sConnect == LOGOFF_BYE) {
                    //..... Display the connection is lost
                    appendToNetData("... Sent Log Off message to Host ...")
//                    //..... Perform the Socket closed process
//                    socketStatusCode.value = sConnect
                    //..... Show closed socket
                    //closeSocket (m_socket)

                }
            }
        }
    }

    //==============================================================================================
    //  HTTP functions to access and update Internet bulletin board
    //==============================================================================================
    fun getHttpSessionData(context: AppCompatActivity, sGameID: String) {

        //..... Save data to be used later
        m_context = context
        m_sGameID = sGameID

        setSessionStatus(SESSION_TABLE_STARTED)

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(context)
        val sURL = HTTP_SESSION_SHOW + m_sGameID

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

    ////////////////////////////////////////////////////////////////////////////////////////////
    //  Must get context from MainActivity.
    //  Although this function does not need context, the Volley HTTP function needs it.
    ////////////////////////////////////////////////////////////////////////////////////////////
    fun httpGetSessionData(context: AppCompatActivity, sGameID: String) {

        //..... Save context
        m_context = context

        viewModelScope.launch {
            val sResponse = httpPostGetSessionData(sGameID)
            httpResponse.value = sResponse
        }
    }

    suspend fun httpPostGetSessionData (sGameID: String) : String {

        return withContext(Dispatchers.IO) {
            val sURL = HTTP_SESSION_SHOW + sGameID
            val url = URL(sURL)
            return@withContext url.readText(Charset.defaultCharset())
        }
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
            //var sSessionRecord = ""
            while (iSession < m_iSessions) {
                val sSessionRecord = sBuffer.substring(iStart, iEnd)
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

    fun unformatIpAddress(sString: String): String {

        var sIpAddress = ""
        for (i in 0..3) {
            //m_iIpAddressClient[i] = sBuffer.substring (iIpStart, iIpStart+3).toInt()
            val sData = sString.substring(i * 3, (i + 1) * 3)
            val iData = sData.toInt()
            val sIp = iData.toString()
            if (sIpAddress.isNotEmpty()) {
                sIpAddress = sIpAddress + "."
            }
            sIpAddress = sIpAddress + sIp
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

    fun addHost(context: AppCompatActivity, sGameID: String, sHostName: String) {

        m_sGameID = sGameID

        val sUrl = constructUrlAddHost(context, sGameID, sHostName)
        //..... Comment out during testing
        postURL(context, sUrl)
    }

    fun constructUrlAddHost(context: AppCompatActivity, sGameID: String, sHostName: String): String {

        //..... Build the following URL
        //http://www.machida.com/cgi-bin/addhost2.pl?Game=RumNet+HOST=TestHost0001+ADDR=010000000121+PORT=8080
        var sUrl = HTTP_ADD_HOST + sGameID
        val sNameNormalized = normalizeName(sHostName)
        sUrl = sUrl + "+HOST=" + sNameNormalized
        //..... Get local IP address
        var sIpAddress = getLocalIpAddress(context)
        if (sIpAddress.isNullOrEmpty()) {
            sIpAddress = "0.0.0.0"
        }
        m_sLocalIpAddress = sIpAddress
        val sIpAddressNormalized = normalizeIpAddress(sIpAddress)
        var sPort="0000" + SOCKET_PORT.toString()
        sPort = sPort.takeLast(4)
        sUrl = sUrl + "+ADDR=" + sIpAddressNormalized + "+PORT=" + sPort

        return sUrl
    }

    fun addGuest (context: AppCompatActivity, sGameID: String, sHostName: String, sGuestName: String) {

        val sUrl = constructUrlAddGuest(sGameID, sHostName, sGuestName)
        //..... Comment out during testing
        postURL(context, sUrl)
    }

    fun constructUrlAddGuest(sGameID: String, sHostName: String, sGuestName : String): String {

        //..... Build the following URL
        //http://www.machida.com/cgi-bin/addguest2.pl?Game=RumNet+HOST=TestHost0001+GUEST=Guest001****
        var sUrl = HTTP_ADD_GUEST + sGameID
        var sNameNormalized = normalizeName(sHostName)
        sUrl = sUrl + "+HOST=" + sNameNormalized
        sNameNormalized = normalizeName(sGuestName)
        sUrl = sUrl + "+GUEST=" + sNameNormalized

        return sUrl
    }

    fun removeGuest (context: AppCompatActivity, sGameID: String, sHostName: String, sGuestName: String) {

        val sUrl = constructUrlRemoveGuest(sGameID, sHostName, sGuestName)
        postURL(context, sUrl)
    }

    fun httpRemoveGuest (context: AppCompatActivity, sGameID: String, sHostName: String, sGuestName: String) {

        val sUrl = constructUrlRemoveGuest(sGameID, sHostName, sGuestName)
        postURL(context, sUrl)
    }

    fun constructUrlRemoveGuest(sGameID: String, sHostName: String, sGuestName : String): String {

        //..... Build the following URL
        //http://www.machida.com/cgi-bin/addguest2.pl?Game=RumNet+HOST=TestHost0001+GUEST=Guest001****
        var sUrl = HTTP_REMOVE_GUEST + sGameID
        var sNameNormalized = normalizeName(sHostName)
        sUrl = sUrl + "+HOST=" + sNameNormalized
        sNameNormalized = normalizeName(sGuestName)
        sUrl = sUrl + "+GUEST=" + sNameNormalized

        return sUrl
    }

    fun postURL(context: AppCompatActivity, sURL: String) {

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(context)
        // Request a string response from the provided URL.
        val stringRequest = StringRequest(
                //..... The following statement changed to remove syntax error per stack overflow
                //      https://stackoverflow.com/questions/32228877/cannot-resolve-symbol-method
                //DownloadManager.Request.Method.GET, sURL,
                Request.Method.GET, sURL,
                { response ->
                    m_sHttpBuffer = response
                },
                { m_sHttpBuffer = sURL + "<-- didn't work!" }
        )
        // Add the request to the RequestQueue.
        queue.add(stringRequest)

    }

    fun httpPostURL(context: AppCompatActivity, sURL: String) {

        viewModelScope.launch {
            val sResponse = httpPostURL2(sURL)
            httpResponseRemoveHost.value = sResponse
        }
    }

    suspend fun httpPostURL2 (sURL: String) : String {

        return withContext(Dispatchers.IO) {
            //val sURL = HTTP_SESSION_SHOW + sGameID
            val url = URL(sURL)
            return@withContext url.readText(Charset.defaultCharset())
        }
    }

    fun getLocalIpAddress(context: AppCompatActivity): String? {
        //val wifiManager = (applicationContext.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager)
        val wifiManager = context.getApplicationContext().getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipInt = wifiInfo.ipAddress
        return InetAddress.getByAddress(
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()
        ).hostAddress
    }

    //..... normalizedName adds trailing "*" to make the name 12 characters
    //      Example: HostName -> HostName****
    fun normalizeName(sName: String): String {

        var sNameNormalized = sName
        val iLength = sName.length
        val iSize = BYTE_SIZE_PLAYER_NAME - iLength
        if (iSize > 0) {
            sNameNormalized = sNameNormalized.padEnd(BYTE_SIZE_PLAYER_NAME, '*')
        } else if (iSize < 0) {
            sNameNormalized = sNameNormalized.take(BYTE_SIZE_PLAYER_NAME)
        }

        return sNameNormalized
    }

    //..... normalizedIpAddress converts 10.0.0.121 to 010000000121
    fun normalizeIpAddress(sIpAddress: String): String {

        val sAddresses = sIpAddress.split(".")
        var sNormalizedIpAddress = ""
        sAddresses.forEach {
            var sAddr = "00" + it
            sAddr = sAddr.takeLast(3)
            sNormalizedIpAddress = sNormalizedIpAddress + sAddr
        }

        return sNormalizedIpAddress
    }

    fun setSessionStatus(iStatus: Int) {
        m_iSessionTableStatus = iStatus
    }

    //==================================================================================================================
    //      Socket operations (Guest)
    //==================================================================================================================

    fun connectToServer(iSessionID: Int, sNickname: String) {

        val sServerIpAddress = m_sessionTable[iSessionID].sessionHostIpAddressLocal
        val iServerPort = m_sessionTable[iSessionID].sessionHostPortNumber

        viewModelScope.launch {
            val sMessage = getSocketData(sServerIpAddress, iServerPort, sNickname)
            //..... 2021/03/18: The following statement will clear all the previous messages if present.
            //                  Consider if this is OK or preserve the previous messages.
            appendToNetData(sMessage)
        }
    }

    suspend fun getSocketData(sServerIpAddress: String, iServerPort: Int, sNickname : String): String {
        var sData: String

        sData = withContext(Dispatchers.IO) {
            //..... NOTE: THe following statement will hang forever if the Server is not running.
            //          A new process to handle such case should be implemented.
            //          For example, pass "Connecting ..." to UI via Bundle to notify the user
            //          and erase with the "Connected ... " message with the return.

            val sSocket = Socket(sServerIpAddress, iServerPort)
            //..... Save this socket to close it in readMessage()
            m_socket = sSocket
            output = PrintWriter(sSocket.getOutputStream())
            input = BufferedReader(InputStreamReader(sSocket.getInputStream()))
            sData = "Connected to " + sServerIpAddress + " (" + iServerPort.toString() + ")"
            putBundleString(SOCKET_STATUS_KEY, SOCKET_CONNECTED)
            //..... Let the server know who I am
            sendloginMessageToServer(sNickname)
            //..... Start a forever loop to get incoming messages from the Server
            m_bOkToTryReadSocket = true
            readMessage()
            //xxxxx The following return of the socket connection may be useless.
            //..... Return the result of the socket connection
            return@withContext sData
        }
        return sData
    }

    fun appendToNetData(sMessage: String) {
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
            //val bundle = Bundle()
            while (m_bOkToTryReadSocket) {
                try {
                    //..... Incoming sMessage now shows who is sending the sMessage
                    val sMessage: String = input.readLine()
                    passMessageToMainThread (sMessage)
                } catch (e: Exception) {
                    //..... Is this exception the result of the DISCONNECT Button?
                    if (m_bOkToTryReadSocket) {
                        //..... No, the connection to the server is lost
                        putBundleString(SOCKET_STATUS_KEY, SOCKET_LOST)
                        closeSocket(m_socket)
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

            //..... Show this message on the UI Textview
            //putBundleString(MESSAGE_KEY, sMessage)
            passMessageToMainThread(sMessage)
        }
    }

    fun sendloginMessageToServer(sNickname: String) {

        thread(start = true) {

            val sMessage = sNickname + ": " + LOGIN_HELLO
            //..... Send the message
            output.write(sMessage)
            //..... 2021/03/08: Must put a CR to complete sending the message
            output.println()
            output.flush()

            //..... This message is not shown to the User
            //putBundleString(MESSAGE_KEY, sMessage)
        }
    }

    fun sendLogOffMessage (sNickname: String) {

        thread(start = true) {

            val sMessage = sNickname + ": " + LOGOFF_BYE
            //..... Send the message
            output.write(sMessage)
            //..... 2021/03/08: Must put a CR to complete sending the message
            output.println()
            output.flush()

            //..... This message is not shown to the User
            //putBundleString(MESSAGE_KEY, sMessage)
            putBundleString(SOCKET_STATUS_KEY, LOGOFF_BYE)

            closeSocket (m_socket)

        }
    }

    fun closeSocket(socket: Socket) {

        //..... Get IP address of this socket
        val inetAddress = socket.inetAddress
        var sMessage = inetAddress.hostAddress

        m_bOkToTryReadSocket = false
        socket.close()
        //..... Notify MainActivity
        sMessage = SOCKET_CLOSED + " (" + sMessage + ")"
        //putBundleString(SOCKET_STATUS_KEY, SOCKET_CLOSED)
        putBundleString(SOCKET_STATUS_KEY, sMessage)

    }

    //==================================================================================================================
    //      Socket operations (Server)
    //==================================================================================================================

    //fun startServerThread(iSessionID: Int, sHostName : String) {
    fun startServerThread(sHostName : String) {

        //val serverSocket : ServerSocket
        var guestSocket : Socket
        var iGuest = -1
        var iError = 0
        var sError = ""
        //var sGuestIpAddress = ""
        var sMessage: String

        //val iSessionIndex = iSessionID - 1
        //..... In future, we need to create a sessionTable[iSessionIndex]
        m_sHostName = sHostName

        //m_serverSocket = ServerSocket(SOCKET_PORT)
        m_serverSocket = ServerSocket(SOCKET_PORT)
        m_bOKToAcceptServerSocket = true

        thread (start = true) {
            //..... Start accept loop for clients
            while (m_bOKToAcceptServerSocket) {

                try {
                    guestSocket = m_serverSocket.accept()
                    //..... Add this guestSocket to the guest socket table
                    //      NOTE: addGuestSocket not only add this guest socket to the table
                    //      but also creates input & output objects for this Guest
                    iGuest = addGuestSocket(guestSocket)
                    if (iGuest < 0) {
                        //..... Do not accept this socket; m_guestSockets[] are full
                        iError = -1
                        sError = "Err001: Too many guests; " + m_sGuestIpAddress[iGuest] + " NOT Accepted\n"
                        declineConnection (guestSocket, m_sGuestIpAddress[iGuest])
                    }
                } catch (e: IOException) {
                    //..... Server Socket closed ...
                    iError = -101
                    sError = "=== ServerSocket closed ===\n"
                    //..... Stop the accept loop
                    m_bOKToAcceptServerSocket = false
                }

                if (iError < 0) {
                    sMessage = sError
                    //..... Show the error message to the UI thread

                    passMessageToMainThread (sMessage)
                } else {
                    //..... Successfully accepted this Guest
                    sMessage = "Connecting to " + m_sGuestIpAddress[iGuest] + "..."
                    passMessageToMainThread (sMessage)
                    //..... Start the read operation for this Guest
                    readGuestMessage (iGuest)
                }
            }
        }

    }

    fun declineConnection (socket: Socket, sIpAddress: String) {

        val output = PrintWriter(socket.getOutputStream())
        var sMessage = "*** Sorry, this session is full ***"
        output.write(sMessage)
        //..... 2021/03/08: Must put a CR to complete sending the message
        output.println()
        output.flush()

        //..... Display this event in Logcat
        sMessage = "*** " + sIpAddress + " rejected; " + sMessage
        Log.i(LOG_CHATTER, sMessage)
    }

    fun passMessageToMainThread (sMessage: String) {

//        val bundle = Bundle()
//
//        bundle.putString(MESSAGE_KEY, sMessage)
//        Message().also {
//            it.data = bundle
//            handler.sendMessage(it)
//        }
        putBundleString (MESSAGE_KEY, sMessage)
    }

    fun putBundleString(sMessageKey: String, sMessage: String) {

        val bundle = Bundle()
        bundle.putString(sMessageKey, sMessage)
        Message().also {
            it.data = bundle
            handler.sendMessage(it)
        }

    }

    /******************************************************************************
     2021/06/06: addGuestSocket design changed.
     The m_guestSocket[iGuest] is no longer moved when a Guest signs out a Chatter session.
     This is because moving m_guestSocket[iGuest] while the Socket is being read will result in
     inconsistency.
     Instead of moving m_guestSocket[] entries, only the m_bOKTOReadGuestSocket[i] is set to false
     for the Guest who signs off. When it is off, m_GuestSocket[] is considered to be available for
     a new Guest sign-in.
     This change affects the process logic in addGuestSocket() and removeGuestSocket()
     and possible other process related to Guest Socket related functions.

     The return value of addGuestSocket() remains the same:
        -1 if the Guest Socket table is full
        iGuest if the newly Guest is added successfully.
    *******************************************************************************/
    fun addGuestSocket(guestSocket: Socket): Int {

        //..... Verify we have an open slot for this potential Guest
        val iGuest = findGuestSocketAvailable()
        if (iGuest < 0) {
            return iGuest
        }

        m_guestSocket[iGuest]           = guestSocket
        m_guestInput[iGuest]            = BufferedReader(InputStreamReader(guestSocket.getInputStream()))
        m_guestOutput[iGuest]           = PrintWriter(guestSocket.getOutputStream())
        m_bOKToReadGuestSocket[iGuest]  = true
        val inetAddress                 = guestSocket.inetAddress
        m_sGuestIpAddress[iGuest]       = inetAddress.hostAddress
        //..... Set m_bGuestAocketAccepted to false until sign in
        m_bGuestSocketAccepted[iGuest]  = false
        m_iGuestSockets += 1

        return iGuest
    }

    fun findGuestSocketAvailable () : Int {

        var iResult = -1

        //..... m_iGuestSockets must be zero to (MAX_GUESTS - 1) to add to the socket table
        if (m_iGuestSockets >= MAX_GUESTS) {
            return iResult
        }
        for (iGuest in 0 until MAX_GUESTS) {
            if (!m_bOKToReadGuestSocket[m_iGuestSockets]) {
                iResult = iGuest
                return iResult
            }
        }
        return iResult
    }

    fun removeGuestSocket(iGuest: Int): Int {

        var iResult = -1

        //..... Do Range check (iGuest zero-relative) to avoid error condition
        if (iGuest < 0 || iGuest >= m_iGuestSockets) {
            //iResult = -1
            return iResult
        }
        //..... Make sure this socket is open
        if (!m_bOKToReadGuestSocket[iGuest]) {
            iResult = -2
            return iResult
        }

        closeGuestSocket (iGuest)

        m_iGuestSockets -= 1
        iResult = m_iGuestSockets
        return iResult
    }

    fun countGuests() : Int {

        var iGuests = 0

        for (i in 0 until MAX_GUESTS) {
            if (m_bOKToReadGuestSocket[i]) {
                iGuests += 1
            }
        }
        return iGuests
    }

    fun welcomeGuest(iGuest: Int) {

        val sMessage = m_sHostName + ": " + m_sGuestName[iGuest] + " has joined this session ..."
        //..... Also, show the message in the UI text
        //putBundleString(MESSAGE_KEY, sMessage)
        //passMessageToMainThread (sMessage)
        broadcastMessage (sMessage, MESSAGE_HOST_ORIGINATED)

    }

    fun notifyGuestLogOff (sGuestName: String) {

        val sMessage = sGuestName + ": has signed off ..."
        //..... Show the message in the UI text
        //putBundleString(MESSAGE_KEY, sMessage)
        //passMessageToMainThread (sMessage)
        broadcastMessage (sMessage, MESSAGE_HOST_ORIGINATED)

    }

    fun broadcastMessage(sMessage: String, iGuestOriginated: Int) {

        //..... iGuestOriginated == MESSAGE_HOST_ORIGINATED means it originated from the Host
        thread(start = true) {
            passMessageToMainThread(sMessage)

            for (iGuest in 0 until m_iGuestSockets) {
                //..... Skip the sending Guest
                if (iGuest != iGuestOriginated  && m_bOKToReadGuestSocket[iGuest]) {
                    sendGuestMessage(iGuest, sMessage)
                }
            }
        }
    }

    fun sendGuestMessage(iGuest: Int, sMessage: String) {

        thread(start = true) {
            val output = m_guestOutput[iGuest]
            //..... Send the message
            output?.write(sMessage)
            //..... 2021/03/08: Must put a CR to complete sending the message
            output?.println()
            output?.flush()
            //..... This is done once in broadcastMessage()
            //putBundleString(MESSAGE_KEY, sMessage)
        }
    }

    fun readGuestMessage(iGuest: Int) {

        val sGuest = m_guestSocket[iGuest]
        val input = m_guestInput[iGuest]
        var sMessage: String

        thread(start = true) {
            if (input != null) {
                //val bundle = Bundle()
                while (m_bOKToReadGuestSocket[iGuest]) {
                    try {
                        sMessage = input.readLine()
                        //..... Recognize & store this Guest Nickname if we haven't done it already
                        if (m_sGuestName[iGuest] == GUEST_NAME_EMPTY) {
                            //..... Remember this Guest name
                            val sGuestName = extractGuestNickname(sMessage)
                            if (sGuestName.isNotEmpty() && m_sGuestName[iGuest] == GUEST_NAME_EMPTY) {
                                val iResult = checkDuplicateName (sGuestName)
                                //..... If the name of this new Guest has already logged in, decline the connection.
                                if (iResult == GUEST_NAME_OK) {
                                    m_sGuestName[iGuest] = sGuestName
                                }
                                else {
                                    rejectDuplicateGuestName (sGuestName, iGuest)
                                }
                            }
                        }

                        //..... If this is a log protocol message from Guest, handle it differently
                        val sMessageBody = getMessageBody(sMessage)
                        if (sMessageBody == LOGIN_HELLO) {
                            addGuest (m_context, m_sGameID, m_sHostName, m_sGuestName[iGuest])
                            welcomeGuest(iGuest)
                        }
                        else
                        if (sMessageBody == LOGOFF_BYE) {
                            val sGuestName = m_sGuestName[iGuest]
                            removeGuest (m_context, m_sGameID, m_sHostName, sGuestName)
                            removeGuestSocket (iGuest)
                            notifyGuestLogOff (sGuestName)
                            //..... Stop the input.readLine() loop
                            m_bOKToReadGuestSocket[iGuest] = false
                        }
                        else
                        {
                            //passMessageToMainThread(sMessage)
                            //..... Broadcast sMessage to the Guests exclusing the originator
                            broadcastMessage(sMessage, iGuest)
                        }
                    } catch (e: Exception) {
                        //..... Is this exception the result of the DISCONNECT Button?
                        if (m_bOKToReadGuestSocket[iGuest]) {
                            //..... No, the connection to the server is lost
                            sMessage = SOCKET_LOST + " to " + m_sGuestName[iGuest]
                            putBundleString(SOCKET_STATUS_KEY, sMessage)
                            closeSocket(sGuest)
                            //..... Stop the input.readLine() loop
                            m_bOKToReadGuestSocket[iGuest] = false
                            removeGuest (m_context, m_sGameID, m_sHostName, m_sGuestName[iGuest])
                            //e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    fun checkDuplicateName (sGuestName : String) : Int {

        var iResult = GUEST_NAME_OK

        for (i in 0 until MAX_GUESTS) {
            if (sGuestName == m_sGuestName[i]) {
                iResult = i
                break
            }
        }

        return iResult
    }
    ///////////////////////////////////////////////////////////////////////////////////////////
    //  declineConnection (iGuest) does the following
    //  (1) Send a rejection message to this Guest
    //  (2) Remove the Socket entry for this Guest
    //  (3) Notify this event to the UI activity
    //  (4) Do not issue another input.readLine() on this socket
    //      That is, set m_bOKToREadGuestSocket[iGuest] = false
    ///////////////////////////////////////////////////////////////////////////////////////////
    fun rejectDuplicateGuestName (sName: String, iGuest : Int) {

        var sMessage = "*** " + sName + " is already used; Connection declied ***"
        sendGuestMessage (iGuest, sMessage)
        passMessageToMainThread(sMessage)
        removeGuestSocket(iGuest)
        sMessage = "=== Socket to " + m_sGuestIpAddress[iGuest] + " closed ==="
        passMessageToMainThread(sMessage)
    }

    fun extractGuestNickname (sMessage: String) : String {
        var sNickname = ""
        val iLength = sMessage.length - 1
        for (i in 0..iLength) {
            if (sMessage[i] == ':') {
                sNickname = sMessage.substring (0, i)
                break
            }
        }

        return sNickname
    }

    fun getMessageBody (sMessage: String) : String {

        var sMessageBody = ""
        val iLength = sMessage.length - 1
        for (i in 0..iLength) {
            if (sMessage[i] == ':') {
                sMessageBody = sMessage.substring (i+2, iLength+1)
                break
            }
        }

        return sMessageBody
    }

    fun shutdownHost(context: AppCompatActivity, sGameID: String, sHostName: String) {

        //..... removeHost2() is now done as the last step because it will start getSession
        //removeHost2 (context, sGameID, sHostName)
        //..... Close all Client sockets if still opened
        closeAllGuestSockets()
        //..... Close ServerSocket
        m_serverSocket.close()
        //..... Do removeHost2 (), which will start netViewModel.httpGetSessionData(this, m_sGameID)
        //      in netViewModel.httpResponseRemoveHost.observe(this, { ... }) in MainActivity
        removeHost2 (context, sGameID, sHostName)
    }

    //fun shutdownConnectionToHost(context: AppCompatActivity, sGameID: String, sGuestName: String) {
    fun shutdownConnectionToHost(sGuestName: String) {

        sendLogOffMessage (sGuestName)
        //..... This operation must be performed by the Host after the Host gets the log off message.
        //removeGuest (context, sGameID, m_sHostName, sGuestName)
        //..... This operation is now down after completion of the SendLogOffMessage.
        //closeSocket (m_socket)

    }

    fun removeHost(context: AppCompatActivity, sGameID: String, sHostName: String) {

        val sUrl = constructUrlRemoveHost(sGameID, sHostName)
        //..... Comment out during testing
        postURL(context, sUrl)
    }

    fun removeHost2(context: AppCompatActivity, sGameID: String, sHostName: String) {

        val sUrl = constructUrlRemoveHost(sGameID, sHostName)
        //..... Comment out during testing
        httpPostURL(context, sUrl)
    }

    fun constructUrlRemoveHost(sGameID: String, sHostName: String): String {

        //..... Build the following URL
        //http://www.machida.com/cgi-bin/remhost2.pl?Game=RumNet+HOST=TestHost0001
        var sUrl = HTTP_REMOVE_HOST + sGameID
        val sNameNormalized = normalizeName(sHostName)
        sUrl = sUrl + "+HOST=" + sNameNormalized

        return sUrl
    }

    fun closeAllGuestSockets () {
        if (m_iGuestSockets > 0) {
            for (i in 0 until m_iGuestSockets) {
                closeGuestSocket (i)
            }
        }
    }

    fun closeGuestSocket (iGuest: Int) {

        if (iGuest < 0 || iGuest >= m_iGuestSockets)
            return
        m_bOKToReadGuestSocket[iGuest] = false
        val socket = m_guestSocket[iGuest]
        socket.close()
        m_sGuestName[iGuest] = GUEST_NAME_EMPTY
    }


}