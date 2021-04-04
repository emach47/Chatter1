package com.example.chatter1

import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley



const val MAX_SESSIONS = 20

//..... MAX_MEMBERS reserved for physical space
const val MAX_MEMBERS_SPACE  = 8
//..... For RumNet, actually used
const val MAX_MEMBERS  = 4

//..... HTTP Buffer data location (zero-relative)
//      Control Record
const val BYTE_BEGIN_OK                 = 0
const val BYTE_SIZE_OK                  = 2
const val BYTE_END_OK                   = BYTE_BEGIN_OK + BYTE_SIZE_OK  // = 2
const val BYTE_BEGIN_ERROR              = BYTE_END_OK + 1               // = 3 Including ":"
const val BYTE_SIZE_ERROR               = 4
const val BYTE_END_ERROR                = BYTE_BEGIN_ERROR + BYTE_SIZE_ERROR // = 7
const val BYTE_BEGIN_SESSIONS           = BYTE_END_ERROR + 1            // = 8 including "/"
const val BYTE_SIZE_SESSIONS            = 2
const val BYTE_END_SESSIONS             = BYTE_BEGIN_SESSIONS + BYTE_SIZE_SESSIONS // = 9
const val BYTE_BEGIN_IP_ADDRESS_CLIENT  = BYTE_END_SESSIONS + 1         // = 11 including " "
const val BYTE_SIZE_IP_ADDRESS          = 12
//..... Session Record(s)
const val BYTE_BEGIN_SESSION_RECORD     = 24                            // = 11+12+1 including CR
const val BYTE_SIZE_SESSION_RECORD      = 129 + 2 + 1                   // = 132 including 2 spaces & CR
//..... Within each Session Record
const val BYTE_BEGIN_SESSION_ID         = 0
const val BYTE_SIZE_SESSION_ID          = 2
const val BYTE_BEGIN_SESSION_MEMBERS    = BYTE_BEGIN_SESSION_ID + BYTE_SIZE_SESSION_ID + 1
                                                                       // = 3 including ":"
const val BYTE_SIZE_SESSION_MEMBERS     = 2
const val BYTE_BEGIN_SESSION_HOST_IP_ADDRESS_LOCAL = BYTE_BEGIN_SESSION_MEMBERS + BYTE_SIZE_SESSION_MEMBERS
                                                                       // = 5
const val BYTE_BEGIN_SESSION_HOST_PORT_NUMBER = BYTE_BEGIN_SESSION_HOST_IP_ADDRESS_LOCAL + BYTE_SIZE_IP_ADDRESS
                                                                       // = 17 = 5+12
const val BYTE_SIZE_SESSION_HOST_PORT_NUMBER = 4
const val BYTE_BEGIN_SESSION_HOST_NAME = BYTE_BEGIN_SESSION_HOST_PORT_NUMBER + BYTE_SIZE_SESSION_HOST_PORT_NUMBER
                                                                       // = 21 = 17+4
const val BYTE_BEGIN_SESSION_GUEST_NAME = BYTE_BEGIN_SESSION_HOST_NAME + BYTE_SIZE_IP_ADDRESS
                                                                      // = 33 = 21+12
const val BYTE_BEGIN_SESSION_HOST_IP_ADDRESS_EXTERNAL = BYTE_BEGIN_SESSION_HOST_NAME + NICKNAME_MAX_SIZE*MAX_MEMBERS_SPACE
                                                                      // = 117 = 21 + 8*12

//const val BYTE_SESSION_RECORDS_START = 32
//const val BYTE_SESSION_RECORD_MEMBERS = 0
//const val BYTE_SESSION_RECORD_HOST_IP_ADDRESS_LOCAL = 2
//const val BYTE_SESSION_RECORD_HOST_NAME = 18
//const val SIZE_PLAYER_NAME = 12
//const val SIZE_IP_ADDRESS = 12
//const val BYTE_SESSION_RECORD_GUEST_NAME = 30
//const val BYTE_SESSION_RECORD_HOST_IP_ADDRESS_EXTERNAL = 114


const val HTTP_SESSION_SHOW = "http://www.machida.com/cgi-bin/show2.pl?Game="

////////////////////////////////////////////////////////////////////////////////////////////
//      NetLogin is a class that holds data and functions to interface the Game Bulletin Board via HTTP
//      by communicating with the Perl scripts in www.machida.com/CGI-BIN directory.
//
//      The purpose of creating this class is to encapsulate the steps necessary to communicate with the Bulletin Board.
//      SessionActivity uses the functions to perform the necessary session processed.
//
////////////////////////////////////////////////////////////////////////////////////////////
//class NetLogin  : AppCompatActivity() {
class NetLogin  {

    lateinit var netViewModel : NetViewModel
    lateinit var m_sSessionID : String


    //..... Result of unformatting m_sHttpBuffer
    var m_sHttpBuffer = ""
    var m_iError = 0
    var m_iSessions = 0                 //Current number of sessions in progress
    var m_iIpAddressClient = Array<Int>(4) {0}
    var m_iPlayers = Array(MAX_SESSIONS, { IntArray(MAX_MEMBERS) })
                                        //Number of participants for each session
    //var m_sPlayerName = Array<Array<String?>>(MAX_SESSIONS) { arrayOfNulls(MAX_MEMBERS) }
    var m_sPlayerName = Array(MAX_SESSIONS) {Array(MAX_MEMBERS){""} }
                                        //Participant names
    var m_iHostIpAddress = Array(MAX_SESSIONS, { IntArray(4) })
                                        //IP Address of each Host

    var m_sessionTable = MutableList(MAX_SESSIONS) {SessionRecord()}

//    override fun onCreate(savedInstanceState: Bundle?) {
//
//        super.onCreate(savedInstanceState)
//    }

//    fun init () {
//
//        netViewModel = NetViewModel()
//    }

/////////////////////////////////////////////////////////////////////////////
//                  CInternetLogin Operations
//    Public routines called from calling routines (typically CView)
/////////////////////////////////////////////////////////////////////////////

    fun getSessionInfo(context: AppCompatActivity, sGameID: String)
    {

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(context)
        val sURL = HTTP_SESSION_SHOW + sGameID

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
                    unformatSessionData (m_sHttpBuffer)
                    //..... move the session data back to SessionActivity
                    moveSessionData ()

                },
                { m_sHttpBuffer = sURL + "<-- didn't work!" })

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    fun getSessionInfo2(context: AppCompatActivity, sGameID: String)
    {

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(context)
        val sURL = HTTP_SESSION_SHOW + sGameID

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
                    unformatSessionData (m_sHttpBuffer)
                    //..... move the session data back to SessionActivity
                    moveSessionData ()
                },
                { m_sHttpBuffer = sURL + "<-- didn't work!" })

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

//
//    fun startSession(context: SessionActivity, sGameID: String) {
//        // Instantiate the RequestQueue.
//        val queue = Volley.newRequestQueue(context)
//        val sURL = HTTP_SESSION_START + sGameID
//
//        // Request a string response from the provided URL.
//        val stringRequest = StringRequest(
//                //..... The following statement chaged to remove syntax error per stack overflow
//                //      https://stackoverflow.com/questions/32228877/cannot-resolve-symbol-method
//                //DownloadManager.Request.Method.GET, sURL,
//                Request.Method.GET, sURL,
//                Response.Listener<String> { response ->
//                    // Display the first 500 characters of the response string.
//                    //textData.text = "Method 1: " + "${response}"
//                    m_sHttpBuffer = "${response}"
//                    unformatSessionData (m_sHttpBuffer)
//
//                },
//                Response.ErrorListener { m_sHttpBuffer = sURL + "<-- didn't work!" })
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

        var sData = ""
        val sIpAddress: String
        val iBufferSize = sBuffer.length

        //..... Unformat Control Record
        val sError  = sBuffer.substring (BYTE_BEGIN_OK, BYTE_END_OK)
        m_iError    = sBuffer.substring (BYTE_BEGIN_ERROR, BYTE_END_ERROR).toInt()
        if (m_iError != 0) {
            return
        }
        // Get the number of sessions in progress
        m_iSessions = sBuffer.substring (BYTE_BEGIN_SESSIONS, BYTE_END_SESSIONS).toInt()
        //..... Get the Client IP Address
        sIpAddress = sBuffer.substring (BYTE_BEGIN_IP_ADDRESS_CLIENT, BYTE_BEGIN_IP_ADDRESS_CLIENT+ BYTE_SIZE_IP_ADDRESS)
        unformatIpAddress(sIpAddress, m_iIpAddressClient)
//        var iIpStart = BYTE_BEGIN_IP_ADDRESS_CLIENT
//        for (i in 0..3) {
//            m_iIpAddressClient[i] = sBuffer.substring (iIpStart, iIpStart+3).toInt()
//            iIpStart = iIpStart + 3
//        }

        //..... Unformat Session Record(s)
        if (m_iSessions > 0) {
            var iSession = 0
            var iStart = BYTE_BEGIN_SESSION_RECORD
            var iEnd = iStart + BYTE_SIZE_SESSION_RECORD
            var sSessionRecord = ""
            var sessionRecord : SessionRecord
            while (iSession < m_iSessions) {
                sSessionRecord = sBuffer.substring (iStart, iEnd)
                sessionRecord = unformatSessionRecord(sSessionRecord)
                m_sessionTable[iSession] = sessionRecord
                iSession++
                iStart = iStart + BYTE_SIZE_SESSION_RECORD
                iEnd = iEnd + BYTE_SIZE_SESSION_RECORD

//                var iPosition = iStart
//                //..... Set Session ID as one-relative number
//                m_sessionTable[iSession].sessionGroupId = iSession + 1
//                //..... Get # of Members in this session
//                sData = sBuffer.substring (iPosition, iPosition+2)
//                m_sessionTable[iSession].sessionMembers = sData.toInt()
//                //..... Get Host name for this session
//                iPosition = iStart + BYTE_SESSION_RECORD_HOST_IP_ADDRESS_LOCAL
//                var sIpAddress = sBuffer.substring (iPosition, iPosition+ SIZE_IP_ADDRESS)
////                val iIpAddress = Array<Int>(4)  {0}
////                unformatIpAddress( sIpAddress, iIpAddress)
////                for (i in 0..3) {
////                    m_sessionTable[iSession].sessionHostIpAddressLocal[i] = iIpAddress[i]
////                }
//                unformatIpAddress( sIpAddress, m_sessionTable[iSession].sessionHostIpAddressLocal)
//
//                //..... Ignore Port number for now
//
//                //..... Get Hos Name
//                iPosition = iStart + BYTE_SESSION_RECORD_HOST_NAME
//                m_sessionTable[iSession].sessionHostName = sBuffer.substring (iPosition, iPosition+SIZE_PLAYER_NAME)
//                //,,,,, Get Member Names
//                if (m_sessionTable[iSession].sessionMembers > 0) {
//                    var iGuest = 0
//                    iPosition = BYTE_SESSION_RECORD_GUEST_NAME
//                    while (iGuest < m_sessionTable[iSession].sessionMembers) {
//                        m_sessionTable[iSession].sessionGuestName[iGuest] = sBuffer.substring (iPosition, SIZE_PLAYER_NAME)
//                        iPosition = iPosition + SIZE_PLAYER_NAME
//                    }
//                }
//                //..... Get Host External IP Address
//                iPosition = iStart + BYTE_SESSION_RECORD_HOST_IP_ADDRESS_EXTERNAL
//                sIpAddress = sBuffer.substring (iPosition, iPosition+ SIZE_IP_ADDRESS)
//                unformatIpAddress( sIpAddress, m_sessionTable[iSession].sessionHostIpAddressExternal)
            }
        }
    }

    fun unformatIpAddress (sIpAddress: String, iIpAddress: Array<Int>) {
        for (i in 0..3) {
            //m_iIpAddressClient[i] = sBuffer.substring (iIpStart, iIpStart+3).toInt()
            val sData = sIpAddress.substring (i*3, (i+1)*3)
            iIpAddress[i] = sData.toInt()
        }
    }

    fun unformatSessionRecord (sSessionRecord: String) : SessionRecord {

        val sessionRecord = SessionRecord()

        //var iPosition = 0
        //..... Set Session ID as one-relative number
        var sData = sSessionRecord.substring (BYTE_BEGIN_SESSION_ID, BYTE_BEGIN_SESSION_ID + BYTE_SIZE_SESSION_ID)
        sessionRecord.sessionGroupId = sData.toInt()
        //..... Get # of Members in this session
        sData = sSessionRecord.substring (BYTE_BEGIN_SESSION_MEMBERS, BYTE_BEGIN_SESSION_MEMBERS+BYTE_SIZE_SESSION_ID)
        val iMembers = sData.toInt()
        sessionRecord.sessionMembers = iMembers
        //..... Get the Host local IP address
        sData = sSessionRecord.substring (BYTE_BEGIN_SESSION_HOST_IP_ADDRESS_LOCAL, BYTE_BEGIN_SESSION_HOST_PORT_NUMBER)
        unformatIpAddress( sData, sessionRecord.sessionHostIpAddressLocal)
        //..... Get the Port Number
        sData = sSessionRecord.substring (BYTE_BEGIN_SESSION_HOST_PORT_NUMBER, BYTE_BEGIN_SESSION_HOST_NAME)
        sessionRecord.sessionHostPortNumber = sData.toInt()
        //..... Get Host name for this session
        sessionRecord.sessionHostName = sSessionRecord.substring (BYTE_BEGIN_SESSION_HOST_NAME, BYTE_BEGIN_SESSION_GUEST_NAME)
        //..... Get the Guest Names if any
        if (iMembers > 1) {
            var i = 0
            var iStart = BYTE_BEGIN_SESSION_GUEST_NAME
            while (i < iMembers) {
                sessionRecord.sessionGuestName[i] = sSessionRecord.substring (iStart, iStart + NICKNAME_MAX_SIZE)
                i++
                iStart = iStart + NICKNAME_MAX_SIZE
            }
        }
        //..... Get Host local IP address
        sData = sSessionRecord.substring (BYTE_BEGIN_SESSION_HOST_IP_ADDRESS_EXTERNAL, BYTE_BEGIN_SESSION_HOST_IP_ADDRESS_EXTERNAL+ BYTE_SIZE_IP_ADDRESS)
        unformatIpAddress( sData, sessionRecord.sessionHostIpAddressExternal)

        return sessionRecord
    }

    fun initNetLogin(sSessionID: String) {
        m_sSessionID = sSessionID


    }

//
//    fun moveSessionData (context: SessionActivity) {
//
//        context.m_iSessions = m_iSessions
//        var i = 0
//        while (i < m_iSessions) {
//            context.m_sessionTable[i] = m_sessionTable[i]
//            i++
//        }
//    }

    fun moveSessionData () {

        netViewModel.m_iError    = m_iError
        netViewModel.m_iSessions = m_iSessions
        var i = 0
        while (i < m_iSessions) {
            netViewModel.m_sessionTable[i] = m_sessionTable[i]
            i++
        }
    }

//    fun requestSessionInformation () {
//
//        //..... Reuest the data showing all sessions in progress
//        //      Note: the result will be processed by ?????
//        m_iHttpRequestCode = HTTP_REQUEST_SESSION_INFORMARION
//        netViewModel.getHttpData()
//
//    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //
    //	The HTTP output (sBuffer) has the following format
    //
    //	Control Data (First Line)
    //	ER:nnnn			if error
    //	or
    //	OK:0000/ii aaabbbcccddd	if successful
    //
    //	where	ii		The total number of games in progress
    //		aaabbbcccddd	The remote address of the requester.
    //
    //	Data Line(s) (Optional: 2nd & successive lines)
    //	nn:mmwwwxxxyyyzzzpppp<Host name 12 char><Guest1 name 12 char> ... rrrssstttuuu
    //
    //	where	nn		The sequential game number
    //		mm		The number of players in this game
    //		wwwxxxyyyzzz	The local address of the HOST
    //		pppp		The port number
    //		rrrssstttuuu	The remote address of the HOST
    //
    ///////////////////////////////////////////////////////////////////////////////////////
//    fun processHttpData(iRequestCode: Int, sBuffer: String) {
//
//        //..... Unformat sBuffer
//        val sResult = sBuffer.substring(BYTE_BEGIN_OK, BYTE_END_OK)
//        //..... Get return code
//        val sError  = sBuffer.substring(BYTE_BEGIN_ERROR, BYTE_END_ERROR)
//        m_iError = safeToInt(sError)
//        if (m_iError != 0) {
//            return
//        }
//        //..... Get the Client's IP Address
//        var iIpStart = BYTE_BEGIN_IP_ADDRESS_CLIENT
//        for (i in 0..3) {
//            m_iIpAddressClient[i] = sBuffer.substring (iIpStart, iIpStart+3).toInt()
//            iIpStart = iIpStart + 3
//        }
//        //..... Get the number of sessions in progress
//        val sSessions = sBuffer.substring(8, 11)
//        m_iSessions = safeToInt(sSessions)
//        if (m_iSessions < 1) {
//            return
//        }
////        //..... Build Session bulletin board data
////        for (iSession in 0..m_iSessions) {
////
////        }
//
//
    }
//
//    fun safeToInt(sString: String) : Int {
//        var iNumber = 0
//        try {
//            iNumber = sString.toInt()
//        } catch (e: Exception){
//            iNumber = 9999
//        }
//        return iNumber
//    }
//}