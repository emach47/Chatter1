package com.example.chatter1

open class SessionRecord {

    var sessionGroupId      = 0
    var sessionMembers      = 0
    var sessionHostName     = ""
//    var sessionHostIpAddressLocal = Array<Int>(4) {0}
//    var sessionHostIpAddressExternal = Array<Int>(4) {0}
    var sessionHostIpAddressLocal = ""
    var sessionHostIpAddressExternal = ""
    var sessionHostPortNumber = 0
    var sessionGuestName = Array<String>(MAX_MEMBERS) {""}
}