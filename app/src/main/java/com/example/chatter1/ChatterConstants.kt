package com.example.chatter1

//..... Intent Key names
const val HTTP_BUFFER = "HTTP Buffer"
const val RETURN_DATA_SESSION_ACTION_KEY = "Session Action"
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

const val BYTE_SIZE_PLAYER_NAME = 12


const val HTTP_SESSION_SHOW = "http://www.machida.com/cgi-bin/show2.pl?Game="
const val HTTP_ADD_HOST = "http://www.machida.com/cgi-bin/addhost2.pl?Game="
