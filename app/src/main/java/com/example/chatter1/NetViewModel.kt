package com.example.chatter1

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.nio.charset.Charset

class NetViewModel : ViewModel() {

    lateinit var m_sSessionID : String
    val httpBuffer = MutableLiveData<String>()

    lateinit var sessionGroups : Array<SessionRecord>

    //--------------------------------------------------------------------------------------------
    //  Data area for Session Infoamation
    //--------------------------------------------------------------------------------------------
    var m_iError = 0
    var m_iSessions = 0                 //Current number of sessions in progress
    var m_iIpAddressClient = Array<Int>(4) {0}
    var m_sessionTable = MutableList(MAX_SESSIONS) {SessionRecord()}


    fun setSessionID (sSessionID: String) {
        m_sSessionID = sSessionID
    }

    fun getHttpData(iRequestCode : Int) {
        viewModelScope.launch {
            httpBuffer.value = getHttpBuffer(iRequestCode)
        }
    }
    suspend fun getHttpBuffer(iRequestCode : Int): String {
        //var httpData: String

        val sUrl = formatUrl(iRequestCode)

        return withContext(Dispatchers.IO) {
            val url = URL(sUrl)
            return@withContext url.readText(Charset.defaultCharset())
        }
    }

    fun formatUrl (iRequestCode: Int) :String {

        var sUrl = ""
        if (iRequestCode == HTTP_REQUEST_SESSION_INFORMARION) {
            sUrl = "http://www.machida.com/cgi-bin/show2.pl?Game=" + m_sSessionID
        }

        return sUrl
    }
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

    fun requestSessionInformation (iRequestCode: Int) {

    }
}