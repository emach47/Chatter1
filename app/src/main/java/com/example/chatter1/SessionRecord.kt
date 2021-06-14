package com.example.chatter1

open class SessionRecord {

    var sessionGroupId      = 0
    var sessionMembers      = 0
    var sessionHostName     = ""
    var sessionHostIpAddressLocal = ""
    var sessionHostIpAddressExternal = ""
    var sessionHostPortNumber = 0
    //var sessionGuestName = Array<String>(MAX_MEMBERS) {""}
    var sessionGuestName = Array<String>(MAX_GUESTS) {""}
}