package com.example.chatter1

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class RealmSessionRecord : RealmObject (){
    @PrimaryKey
    var sessionId: Long = 0
    var sessionMembers      = 0
    var sessionHostName     = ""

    //..... Since Realm does not allow Integer Array, the two IP addresses will be strings
    //var sessionHostIpAddressLocal = Array<Int>(4) {0}
    //var sessionHostIpAddressExternal = Array<Int>(4) {0}
    var sessionHostIpAddressLocal = ""
    var sessionHostIpAddressExternal = ""
    var sessionHostPortNumber = 0

    //..... Since Realm dees not allow String Array, sesstionGuestName will be a string of cocatnated guest names
    //var sessionGuestNames = Array<String> (4) {""}
    var sessionGuestNames = ""
}