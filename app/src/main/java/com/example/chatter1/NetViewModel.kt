package com.example.chatter1

//..... 2021/04/22: the following 2 statements added when viewModelScope.launch {} is coded

import android.net.wifi.WifiManager
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
import java.io.*
import java.net.InetAddress
import java.net.NetworkInterface.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

const val SESSION_TABLE_STARTED = 0
const val SESSION_TABLE_COMPLETED = 1

const val MESSAGE_KEY = "message_data"
const val MESSAGE_KEY_IN = "message_in"
const val MESSAGE_KEY_OUT = "message_out"

//..... Constants for SOCKET operation
const val SOCKET_PORT = 8080

const val SOCKET_STATUS_KEY = "socket_status"
const val SOCKET_CLOSED = "Closed"
const val SOCKET_CONNECTEDED = "Connected"
const val SOCKET_LOST = "Lost"

const val MAX_GUESTS = 3

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
    //  Data for Host Mode
    //==============================================================================================
    lateinit var m_serverSocket: ServerSocket
    var m_iGuestSockets = 0
    var m_guestSocket = MutableList(MAX_GUESTS) { Socket() }
    //..... m_sGuestName needs to be replaced with the correct Nickname upon connection
    var m_sGuestName = arrayOf ("Guest1", "Guest2", "Guest3")
    //..... The following data should be a local data.
    //      To be deleted when startServer() Coroutine function is removed
    var m_sGuestIpAddress = ""

    //var m_guestInput = MutableList (MAX_GUESTS) { InputStream() }
    var m_guestInput:  Array<BufferedReader?> = Array(MAX_GUESTS) { null }
    var m_guestOutput: Array<PrintWriter?> = Array(MAX_GUESTS) { null }


    //==============================================================================================
    //  Data for Guest Mode
    //==============================================================================================
    //..... m_socket for the Guest mode
    //      It is used (only once) to close the socket in readMessage() when the socket to the Host is closed
    lateinit var m_socket: Socket

    //..... 2021/02/28: Added input/output for Guest socket connection
    lateinit var input: BufferedReader
    lateinit var output: PrintWriter

    //..... The switch to stop the forever loop in readMessage() if it it is closed
    var m_bOkToTryReadSocket = true

    //--------------------------------------------------------------------------------------------
    //  Data area for Session Infoamation
    //--------------------------------------------------------------------------------------------
    var m_sGameID = ""
    var m_iSessionTableStatus = 0
    var m_iError = 0
    var m_iSessions = 0                 //Current number of sessions in progress

    //var m_iIpAddressClient = Array<Int>(4) {0}
    var m_sIpAddressClient = ""
    //..... m_sLocalIpAddress is set in constructUrlAddHost(...) by addHost(...)
    var m_sLocalIpAddress = ""

    //..... 2021/05/06: Changed due to use expandable List
    //var m_sessionTable = MutableList(MAX_SESSIONS) {SessionRecord()}
    var m_sessionTable: MutableList<SessionRecord> = mutableListOf<SessionRecord>()
    var m_sHostName = ""
    var m_sHostIpAddress = ""

    var m_sHttpBuffer = ""

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
//    fun getHttpSessionData (sGameID: String) {
//        viewModelScope.launch {
//            m_sGameID = sGameID
//            val sURL = HTTP_SESSION_SHOW + sGameID
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

    fun getHttpSessionData(context: AppCompatActivity, sGameID: String) {

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
//
//    fun buildSessionTable (context: AppCompatActivity, sGameID: String) {
//
//        m_sGameID = sGameID
//
//        setSessionStatus(SESSION_TABLE_STARTED)
//
//        // Instantiate the RequestQueue.
//        val queue = Volley.newRequestQueue(context)
//        val sURL = HTTP_SESSION_SHOW + m_sGameID
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

    fun addHost(context: AppCompatActivity, sGameID: String, sHostName: String, iSessionID: Int) {

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

    fun getLocalIpAddress(context: AppCompatActivity): String? {
        //val wifiManager = (applicationContext.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager)
        val wifiManager = context.getApplicationContext().getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipInt = wifiInfo.ipAddress
        return InetAddress.getByAddress(
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()
        ).hostAddress
    }

    fun getMyLocalIpAddress(): String? {
        try {
            val en = getNetworkInterfaces()
            while (en
                            .hasMoreElements()
            ) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr
                                .hasMoreElements()
                ) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress) {
                        return inetAddress.hostAddress.toString()
                    }
                }
            }
        } catch (ex: SocketException) {

            return (ex.toString());
        }
        return null
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

        var sAddresses = sIpAddress.split(".")
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

    fun connectToServer(iSessionID: Int) {

        val sServerIpAddress = m_sessionTable[iSessionID].sessionHostIpAddressLocal
        val iServerPort = m_sessionTable[iSessionID].sessionHostPortNumber
        m_sHostName = m_sessionTable[iSessionID].sessionHostName
        m_sHostName = m_sHostName.trimEnd()

        viewModelScope.launch {
            val sMessage = getSocketData(sServerIpAddress, iServerPort)
            //..... 2021/03/18: The following statement will clear all the previous messages if present.
            //                  Consider if this is OK or preserve the previous messages.
            //netData.value = sResult
            appendToNetData(sMessage)
        }
    }

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

        m_sHostName = m_sessionTable[0].sessionHostName
        m_sHostIpAddress = m_sessionTable[0].sessionHostIpAddressLocal
        val sPortAddress = m_sessionTable[0].sessionHostPortNumber
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
            putBundleString(SOCKET_STATUS_KEY, SOCKET_CONNECTEDED)
            //..... Start a forever loop to get incoming messages from the Server
            m_bOkToTryReadSocket = true
            readMessage()
            //xxxxx The following return of the socket connection may be useless.
            //..... Return the result of the socket connection
            return@withContext sData
        }
        return sData
    }

    fun putBundleString(sMessageKey: String, sMessage: String) {

        val bundle = Bundle()
        bundle.putString(sMessageKey, sMessage)
        Message().also {
            it.data = bundle
            handler.sendMessage(it)
        }

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
            val bundle = Bundle()
            while (m_bOkToTryReadSocket) {
                try {
                    val sMessage: String = input.readLine()
                    //..... Incoming sMessage now shows who is sending the sMessage
                    //sMessage = "Server: " + sMessage
                    //..... The following statement will not compile
                    //runOnUiThread(Runnable { tvMessages.append("server: $message\n") })
                    //..... The following statement causes a runtime failure
                    //netData.value = netData.value + message + "\n"
//                    bundle.putString(MESSAGE_KEY, sMessage)
//                    Message().also {
//                        it.data = bundle
//                        handler.sendMessage(it)
//                    }
                    passMessage (sMessage)
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

            //..... Send sMessage to UI via bundle
//            bundle.putString(MESSAGE_KEY, sMessage)
//            Message().also {
//                it.data = bundle
//                handler.sendMessage(it)
//            }
            putBundleString(MESSAGE_KEY, sMessage)
        }
    }

    fun closeSocket(socket: Socket) {

        m_bOkToTryReadSocket = false
        socket.close()
        //..... Notify MainActivity
        putBundleString(SOCKET_STATUS_KEY, SOCKET_CLOSED)

    }

    //==================================================================================================================
    //      Socket operations (Server)
    //==================================================================================================================

    fun startServerThread() {

        val bundle = Bundle()

        val serverSocket : ServerSocket
        var guestSocket = Socket()
        var iError = 0
        var sError = ""
        var sGuestIpAddress = ""
        var sMessage = ""

        //m_serverSocket = ServerSocket(SOCKET_PORT)
        serverSocket = ServerSocket(SOCKET_PORT)
        //..... Show Server Mode started
        //      The following notification is now displayed when the User selects to start a new session
        //      processStartSession (iSessionID) in MainActivity because creating m_serverSocket does not fail.
        //sData = "Starting Server mode ..."
        thread (start = true) {
            //..... Start accept loop for multiple clients
            while (true) {

                try {
                    guestSocket = serverSocket.accept()
                    //..... add this guestSocket to the guest socket table
                    //      NOTE: addGuestSocket not only add this guest socket to the table
                    //      but also creates input & output objects for this Guest
                    val inetAddress = guestSocket.inetAddress
                    sGuestIpAddress = inetAddress.hostAddress
                    val iResult = addGuestSocket(guestSocket)
                    if (iResult < 0) {
                        //..... Do not accept this socket; m_guestSockets[] are full
                        iError = -1
                        //sError = "Err001: Too many guests; " + m_sGuestIpAddress + " NOT Accepted\n"
                        sError = "Err001: Too many guests; " + sGuestIpAddress + " NOT Accepted\n"
                    }
                    //..... Guest connection is made
                } catch (e: IOException) {
                    iError = -101
                    sError = "Err101: Failed to accept guest socket\n"
                }

                if (iError < 0) {
                    sMessage = sError
                    //..... Show the error message to the UI thread
                    passMessage (sMessage)
                } else {
                    //..... Successfully accepted this Guest
//                    val inetAddress = guestSocket.inetAddress
//                    sGuestIpAddress = inetAddress.hostAddress
                    sMessage = "Connected to " + sGuestIpAddress + "\n"
                    passMessage (sMessage)

                    //..... Start the read operation for this Guest
                    val iGuest = m_iGuestSockets - 1
                    //..... welcomeGuest sends a welcome message and starts readMessage (iGuest)
                    //      to start the input operation
                    //welcomeGuest(iGuest)
                    readGuestMessage (iGuest)
                }

            }
        }

}

    fun passMessage (sMessage: String) {

        val bundle = Bundle()

        bundle.putString(MESSAGE_KEY, sMessage)
        Message().also {
            it.data = bundle
            handler.sendMessage(it)
        }
    }

    fun startServerSocket2() {

        var sData = ""

        thread (start = true) {
            //..... Do NOT call appendToNetData in this function.
            //      Doing so will result in runtime error.

            //..... NOTE: THe following statement will hang forever if the Server is not running.
            //          A new process to handle such case should be implemented.
            //          For example, pass "Connecting ..." to UI via Bundle to notify the user
            //          and erase with the "Connected ... " message with the return.
            var iError = 0
            var sError = ""
            val guestSocket: Socket
            val guestIpAddress = ""
            try {
                m_serverSocket = ServerSocket(SOCKET_PORT)
                //..... Show Server Mode started
                sData = "Starting Server mode ...\n"
            } catch (e: IOException) {
                iError = -102
                sData = "Err102: Could not start Server Socket\n"
            }
        }

    }

    fun startServer2() {
        thread(start = true) {

            var iError = 0
            var sError = "OK"
            var sData = ""
            var guestSocket = Socket()
            try {
                m_serverSocket = ServerSocket(SOCKET_PORT)
                //..... Show Server Mode started
                sData = "Starting Server mode ..."
                //..... Listen and accept connection from a Guest
                try {
                    guestSocket = m_serverSocket.accept()
                    val iResult = addGuestSocket(guestSocket)
                    if (iResult < 0) {
                        //..... Do not accept this socket; the m_guestSoclets[] are full
                        iError = -1
                        sError = "Err001: Max number of guests exceeded"
                    }
                } catch (e: IOException) {
                    iError = -101
                    sError = "Err101: Failed to aceot guest socket"
                }
            } catch (e: IOException) {
                iError = -102
                sError = "Err102: Could not open Server Socket"
            }
            if (iError < 0) {
                if (iError == -1) {
                    //declineAccept (guestSocket)
                }
                //appendToNetData(sError)
                sData = sError
            } else {
                val inetAddress = guestSocket.inetAddress
                m_sGuestIpAddress = inetAddress.hostAddress
                sData = "Connected to " + m_sGuestIpAddress + "\n"
                //..... Accept this guest
                val iGuest = m_iGuestSockets - 1
                //welcomeGuest (iGuest)
                readGuestMessage (iGuest)
            }
        }
    }

    fun acceptGuestConnection(): String {

        var sData = ""
        var iError = 0
        var sError = ""
        var guestSocket = Socket()
        val guestIpAddress = ""
        val bundle = Bundle()

        thread (start = true) {
            //..... Listen and accept a connection from a Guest
            try {
                guestSocket = m_serverSocket.accept()
                //..... add this guestSocket to the guest socket table
                //      NOTE: addGuestSocket not only add this guest socket to the table
                //      but also creates input & output objects for this Guest
                val iResult = addGuestSocket(guestSocket)
                if (iResult < 0) {
                    //..... Do not accept this socket; m_guestSockets[] are full
                    iError = -1
                    sError = "Err001: Too many guests; " + m_sGuestIpAddress + " NOT Accepted"
                }
                //..... Guest connection is made
            } catch (e: IOException) {
                iError = -101
                sError = "Err101: Failed to accept guest socket"
            }

            if (iError < 0) {
                sData = sError

            } else {
                //appendToNetData("Host Mode started...")
                val inetAddress = guestSocket.inetAddress
                m_sGuestIpAddress = inetAddress.hostAddress
                sData = "Connected to " + m_sGuestIpAddress + "\n"
                //..... Accept this guest
                val iGuest = m_iGuestSockets - 1
                //..... welcomeGuest sends a welcome message and starts readMessage (iGuest)
                //      to start the input operation
                //welcomeGuest(iGuest)
                readGuestMessage (iGuest)
            }
        }

        val sMessage = sData
        bundle.putString(MESSAGE_KEY, sMessage)
        Message().also {
            it.data = bundle
            handler.sendMessage(it)
        }
        return sData
    }

    fun startServer() {

        viewModelScope.launch {
            val sMessage = startServerSocket()
            appendToNetData(sMessage)
        }

    }

    suspend fun startServerSocket(): String {
        var sData = ""

        sData = withContext(Dispatchers.IO) {
            //..... Do NOT call appendToNetData in this function.
            //      Doing so will result in runtime error.

            //..... NOTE: THe following statement will hang forever if the Server is not running.
            //          A new process to handle such case should be implemented.
            //          For example, pass "Connecting ..." to UI via Bundle to notify the user
            //          and erase with the "Connected ... " message with the return.
            var iError = 0
            var sError = ""
            var guestSocket = Socket()
            var sGuestIpAddress = ""
            try {
                m_serverSocket = ServerSocket(SOCKET_PORT)
                //..... Show Server Mode started
                //      The following notification is now displayed when the User selects to start a new session
                //      processStartSession (iSessionID) in MainActivity because creating m_serverSocket does not fail.
                //sData = "Starting Server mode ..."
                try {
                    guestSocket = m_serverSocket.accept()
                    //..... add this guestSocket to the guest socket table
                    //      NOTE: addGuestSocket not only add this guest socket to the table
                    //      but also creates input & output objects for this Guest
                    val iResult = addGuestSocket(guestSocket)
                    if (iResult < 0) {
                        //..... Do not accept this socket; m_guestSockets[] are full
                        iError = -1
                        sError = "Err001: Too many guests; " + m_sGuestIpAddress + " NOT Accepted\n"
                    }
                    //..... Guest connection is made
                } catch (e: IOException) {
                    iError = -101
                    sError = "Err101: Failed to accept guest socket\n"
                }
            } catch (e: IOException) {
                iError = -102
                sError = "Err102: Could not start Server Socket\n"
            }

            if (iError < 0) {
                sData = sError
            } else {
                //..... Successfully accepted this Guest
                val inetAddress = guestSocket.inetAddress
                sGuestIpAddress = inetAddress.hostAddress
                sData = "Connected to " + sGuestIpAddress + "\n"
                m_sGuestIpAddress = sGuestIpAddress
                //..... Accept this guest
                val iGuest = m_iGuestSockets - 1
                //..... welcomeGuest sends a welcome message and starts readMessage (iGuest)
                //      to start the input operation
                //welcomeGuest(iGuest)
                readGuestMessage (iGuest)

            }
            return@withContext sData
        }

        return sData
    }

    fun addGuestSocket(guestSocket: Socket): Int {

        var iResult = 0

        //..... m_iGuestSockets must be zero to (MAX_GUESTS - 1) to add this socket
        if (m_iGuestSockets >= MAX_GUESTS) {
            iResult = -1
        } else {
            m_guestSocket[m_iGuestSockets] = guestSocket
            m_guestInput[m_iGuestSockets] = BufferedReader(InputStreamReader(guestSocket.getInputStream()))
            m_guestOutput[m_iGuestSockets] = PrintWriter(guestSocket.getOutputStream())
            m_iGuestSockets = m_iGuestSockets + 1
            iResult = m_iGuestSockets
        }
        return iResult
    }

    fun countGuests() : Int {

        return m_iGuestSockets
    }

    fun removeGuestSocket (iGuest: Int) : Int {

        var iResult = 0
        var iGuestTotal = m_iGuestSockets
        //..... Do Range check (iGuest zero-relative) to avoid error condition
        if (iGuest >= iGuestTotal) {
            //..... Error: cannot remove this Guest
            iResult = -1
            return iResult
        }
        //..... Do I need to remove only the last entry?
        if (iGuest == (iGuestTotal - 1)) {
            //..... Yes, simply decrement m_iGuestSockets
            iGuestTotal = iGuestTotal - 1
            m_iGuestSockets = iGuestTotal
            iResult = iGuestTotal
            return iResult
        }
        //..... iGuest < (iGuest - 1)
        for (i in iGuest..(iGuestTotal - 1)) {
            m_guestSocket[i] = m_guestSocket[i+1]
            m_sGuestName [i] = m_sGuestName [i+1]
        }
        iGuestTotal = iGuestTotal - 1
        m_iGuestSockets = iGuestTotal
        iResult = iGuestTotal
        return iResult
    }

    fun welcomeGuest(iGuest: Int) {

        sendGuestMessage (iGuest, "Welcome")
        readGuestMessage (iGuest)
    }

    fun broadcastMessage(sMessage: String) {

        thread(start = true) {
            for (iGuest in 0..(m_iGuestSockets - 1)) {
                sendGuestMessage(iGuest, sMessage)
            }
            putBundleString(MESSAGE_KEY, sMessage)
        }
    }

    fun sendGuestMessage(iGuest: Int, sMessage: String) {

        thread(start = true) {
            val output = m_guestOutput[iGuest]
//            val bundle = Bundle()
            //..... Send the message
            output?.write(sMessage)
            //..... 2021/03/08: Must put a CR to complete sending the message
            output?.println()
            output?.flush()
            //..... This is done on broadcastMessage()
            //putBundleString(MESSAGE_KEY, sMessage)
        }
    }

    fun readGuestMessage(iGuest: Int) {

        val sSocket = m_guestSocket[iGuest]
        val input = m_guestInput[iGuest]

        thread(start = true) {
            if (input != null) {
                val bundle = Bundle()
                while (m_bOkToTryReadSocket) {
                    try {
                        val sMessage: String = input.readLine()
                        //..... sMessage now has the preceding name
                        //sMessage = m_sGuestName[iGuest] + ": " + sMessage
                        //..... The following statement will not compile
                        //runOnUiThread(Runnable { tvMessages.append("server: $message\n") })
                        //..... The following statement causes a runtime failure
                        //netData.value = netData.value + message + "\n"
                        bundle.putString(MESSAGE_KEY, sMessage)
                        Message().also {
                            it.data = bundle
                            handler.sendMessage(it)
                        }
                    } catch (e: Exception) {
                        //..... Is this exception the result of the DISCONNECT Button?
                        if (m_bOkToTryReadSocket) {
                            //..... No, the connection to the server is lost
                            putBundleString(SOCKET_STATUS_KEY, SOCKET_LOST)
                            closeSocket(sSocket)
                            e.printStackTrace()
                            //..... Stop the readLine() loop
                            m_bOkToTryReadSocket = false
                        }
                    }
                }
            }
        }
    }

    fun shutdownHost(context: AppCompatActivity, sGameID: String, sHostName: String) {

        removeHost (context, sGameID, sHostName)
        //..... Close all Client sockets if still opened
        closeAllGuestSockets()
        //..... Close ServerSoclet
        m_serverSocket.close()

    }

    fun removeHost(context: AppCompatActivity, sGameID: String, sHostName: String) {

        val sUrl = constructUrlRemoveHost(context, sGameID, sHostName)
        //..... Comment out during testing
        postURL(context, sUrl)
    }

    fun constructUrlRemoveHost(context: AppCompatActivity, sGameID: String, sHostName: String): String {

        //..... Build the following URL
        //http://www.machida.com/cgi-bin/remhost2.pl?Game=RumNet+HOST=TestHost0001
        var sUrl = HTTP_REMOVE_HOST + sGameID
        val sNameNormalized = normalizeName(sHostName)
        sUrl = sUrl + "+HOST=" + sNameNormalized

        return sUrl
    }

    fun closeAllGuestSockets () {
        if (m_iGuestSockets > 0) {
            for (i in 0..(m_iGuestSockets - 1)) {
                closeGuestSocket (i)
            }
        }
    }

    fun closeGuestSocket (iGuest: Int) {

    }


}