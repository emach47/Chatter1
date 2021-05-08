package com.example.chatter1



////////////////////////////////////////////////////////////////////////////////////////////
//      NetLogin is a class that holds data and functions to interface the Game Bulletin Board via HTTP
//      by communicating with the Perl scripts in www.machida.com/CGI-BIN directory.
//
//      The purpose of creating this class is to encapsulate the steps necessary to communicate with the Bulletin Board.
//      SessionActivity uses the functions to perform the necessary session processed.
//
////////////////////////////////////////////////////////////////////////////////////////////

//===== 2021/04/30: NetLogin Effectively removed =====
class NetLogin {

//    val netViewModel = NetViewModel()
//    lateinit var m_sSessionID: String
//
//
//    //..... Result of unformating m_sHttpBuffer
//    var m_sHttpBuffer = ""
//    var m_iError = 0
//    var m_iSessions = 0                 //Current number of sessions in progress
//    var m_iIpAddressClient = Array<Int>(4) { 0 }
//
//    var m_sessionTable = MutableList(MAX_SESSIONS) { SessionRecord() }

/////////////////////////////////////////////////////////////////////////////
//                  CInternetLogin Operations
//    Public routines called from calling routines (typically CView)
/////////////////////////////////////////////////////////////////////////////

//    fun getSessionInfo(context: AppCompatActivity, sGameID: String) {
//
//        setSessionStatus(SESSION_TABLE_STARTED)
//
//        // Instantiate the RequestQueue.
//        val queue = Volley.newRequestQueue(context)
//        val sURL = HTTP_SESSION_SHOW + sGameID
//
//        // Request a string response from the provided URL.
//        val stringRequest = StringRequest(
//                //..... The following statement chaged to remove syntax error per stack overflow
//                //      https://stackoverflow.com/questions/32228877/cannot-resolve-symbol-method
//                //DownloadManager.Request.Method.GET, sURL,
//                Request.Method.GET, sURL,
//                { response ->
//                    // Display the first 500 characters of the response string.
//                    //textData.text = "Method 1: " + "${response}"
//                    m_sHttpBuffer = response
//                    //unformatSessionData(m_sHttpBuffer)
//                    //..... move the unformatted session data to NetViewData
//                    moveSessionData()
//                    setSessionStatus(SESSION_TABLE_COMPLETED)
//                },
//                { m_sHttpBuffer = sURL + "<-- didn't work!" })
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
//    fun unformatSessionData(sBuffer: String) {
//
//        //var sData = ""
//        val sIpAddress: String
//        //val iBufferSize = sBuffer.length
//
//        //..... Unformat Control Record
//        //val sError  = sBuffer.substring (BYTE_BEGIN_OK, BYTE_END_OK)
//        m_iError = sBuffer.substring(BYTE_BEGIN_ERROR, BYTE_END_ERROR).toInt()
//        if (m_iError != 0) {
//            return
//        }
//        // Get the number of sessions in progress
//        m_iSessions = sBuffer.substring(BYTE_BEGIN_SESSIONS, BYTE_END_SESSIONS).toInt()
//        //..... Get the Client IP Address
//        sIpAddress = sBuffer.substring(BYTE_BEGIN_IP_ADDRESS_CLIENT, BYTE_BEGIN_IP_ADDRESS_CLIENT + BYTE_SIZE_IP_ADDRESS)
//        unformatIpAddress(sIpAddress, m_iIpAddressClient)
//
//        //..... Unformat Session Record(s)
//        if (m_iSessions > 0) {
//            var iSession = 0
//            var iStart = BYTE_BEGIN_SESSION_RECORD
//            var iEnd = iStart + BYTE_SIZE_SESSION_RECORD
//            var sSessionRecord = ""
//            var sessionRecord: SessionRecord
//            while (iSession < m_iSessions) {
//                sSessionRecord = sBuffer.substring(iStart, iEnd)
//                sessionRecord = unformatSessionRecord(sSessionRecord)
//                m_sessionTable[iSession] = sessionRecord
//                iSession++
//                iStart = iStart + BYTE_SIZE_SESSION_RECORD
//                iEnd = iEnd + BYTE_SIZE_SESSION_RECORD
//            }
//        }
//    }
//
//    fun unformatIpAddress(sIpAddress: String, iIpAddress: Array<Int>) {
//        for (i in 0..3) {
//            //m_iIpAddressClient[i] = sBuffer.substring (iIpStart, iIpStart+3).toInt()
//            val sData = sIpAddress.substring(i * 3, (i + 1) * 3)
//            iIpAddress[i] = sData.toInt()
//        }
//    }
//
//    fun unformatSessionRecord(sSessionRecord: String): SessionRecord {
//
//        val sessionRecord = SessionRecord()
//
//        //..... Set Session ID as one-relative number
//        var sData = sSessionRecord.substring(BYTE_BEGIN_SESSION_ID, BYTE_BEGIN_SESSION_ID + BYTE_SIZE_SESSION_ID)
//        sessionRecord.sessionGroupId = sData.toInt()
//        //..... Get # of Members in this session
//        sData = sSessionRecord.substring(BYTE_BEGIN_SESSION_MEMBERS, BYTE_BEGIN_SESSION_MEMBERS + BYTE_SIZE_SESSION_ID)
//        val iMembers = sData.toInt()
//        sessionRecord.sessionMembers = iMembers
//        //..... Get the Host local IP address
//        sData = sSessionRecord.substring(BYTE_BEGIN_SESSION_HOST_IP_ADDRESS_LOCAL, BYTE_BEGIN_SESSION_HOST_PORT_NUMBER)
//        unformatIpAddress(sData, sessionRecord.sessionHostIpAddressLocal)
//        //..... Get the Port Number
//        sData = sSessionRecord.substring(BYTE_BEGIN_SESSION_HOST_PORT_NUMBER, BYTE_BEGIN_SESSION_HOST_NAME)
//        sessionRecord.sessionHostPortNumber = sData.toInt()
//        //..... Get Host name for this session
//        sessionRecord.sessionHostName = sSessionRecord.substring(BYTE_BEGIN_SESSION_HOST_NAME, BYTE_BEGIN_SESSION_GUEST_NAME)
//        //..... Get the Guest Names if any
//        if (iMembers > 1) {
//            var i = 0
//            var iStart = BYTE_BEGIN_SESSION_GUEST_NAME
//            while (i < iMembers) {
//                sessionRecord.sessionGuestName[i] = sSessionRecord.substring(iStart, iStart + NICKNAME_MAX_SIZE)
//                i++
//                iStart = iStart + NICKNAME_MAX_SIZE
//            }
//        }
//        //..... Get Host local IP address
//        sData = sSessionRecord.substring(BYTE_BEGIN_SESSION_HOST_IP_ADDRESS_EXTERNAL, BYTE_BEGIN_SESSION_HOST_IP_ADDRESS_EXTERNAL + BYTE_SIZE_IP_ADDRESS)
//        unformatIpAddress(sData, sessionRecord.sessionHostIpAddressExternal)
//
//        return sessionRecord
//    }
//
//    fun initNetLogin(sSessionID: String) {
//        m_sSessionID = sSessionID
//   }

//    fun setSessionStatus (iStatus: Int) {
//        netViewModel.m_iSessionTableStatus = iStatus
//    }
//
//    fun moveSessionData() {
//
//        //..... Since netViewModel has never been initialized, do it here
//        //netViewModel = NetViewModel()
//
//        netViewModel.m_iError = m_iError
//        netViewModel.m_iSessions = m_iSessions
//        var i = 0
//        while (i < m_iSessions) {
//            netViewModel.m_sessionTable[i] = m_sessionTable[i]
//            i++
//        }
//    }

}