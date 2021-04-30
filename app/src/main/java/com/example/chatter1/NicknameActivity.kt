package com.example.chatter1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.trimmedLength
import androidx.preference.PreferenceManager

const val NICKNAME_MAX_SIZE = 12

//const val RETURN_CODE_NICKNAME = 1234
//const val RETURN_DATA_KEY = "Nickname"

class NicknameActivity : AppCompatActivity() {

    lateinit var m_textNicknameHint : TextView
    lateinit var m_textError : TextView
    lateinit var m_editNickname : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nickname)

        m_textNicknameHint      = findViewById(R.id.textViewHint)
        m_textError             = findViewById(R.id.textViewError)
        m_editNickname          = findViewById(R.id.editNickname)

        //..... Check if this is to create a new Nickname or change an existing one
        //xxxxx 2021/03/20: intent.getStringExtra returns null and I cannot get to wotk.
        //      bundle and sNickname both get null
        //val intent = Intent()
        //val bundle = intent.extras
        //var sNickname = intent.getStringExtra (NICKNAME_KEY).toString()
        //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
        //..... So, this my stop-gap solution for now.
        var sNickname = getNicknameFromDevice ()
        if (sNickname == "null" || sNickname.isEmpty()) {
            sNickname = ""
            m_textNicknameHint.text = getString(R.string.nickname_new)
        } else {
            m_textNicknameHint.text = getString(R.string.nickname_confirm)
        }
        //..... Set the initial value for m_editNickname

        m_editNickname.setText(sNickname)

    }

    //..... 2021/03/20: Copied from MainActivity to go aroung the issue mentioned above.
    //      But it should be removed when the issue is resolved
    fun getNicknameFromDevice () : String {

        lateinit var sNickname : String

        //..... Get the Nickname from SharedPreference
        ////////////////////////////////////////////////////////////////////////////////////////////
        //      NOTE: The following statement resulted in deprecated indication initially.
        //      To alleviate this issue, take the following steps
        //      (1) In build.gradle (:app), add the following statement
        //          implementation 'androidx.preference:preference-ktx:1.1.1'
        //      (2) Change the import statement at the start of this source program from android.... to androidx....
        //          import androidx.preference.PreferenceManager
        ////////////////////////////////////////////////////////////////////////////////////////////
        PreferenceManager.getDefaultSharedPreferences(this).apply {
            sNickname = getString (NICKNAME_KEY, "").toString()
        }
        return sNickname
    }

    fun onClickButtonOK (view : View) {

        val sNickname = m_editNickname.text

        //..... Edit chack on the Nickname
        if (sNickname.isNullOrEmpty()) {
            m_textError.text = "Error: Please enter Nickname"
            return
        }
        else if (sNickname.trimmedLength() > NICKNAME_MAX_SIZE) {
            m_textError.text = "Error: Nickname size must be 12 or less"
            return
        }

        //..... Erase any error message
        m_textError.text =""
        //..... 2021/03/20: The intent.puExtra () requires .toString when passing the sNickName
        //      Otherwise the onActivityResult() in MainActivity will get null on sNickname.
        //      In both Olymoic3 & Kotlin14C, the string data were converted using .toString() prior to putExtra().
        //      This solution was found in Stack Overflow. The URL is below
        //          https://stackoverflow.com/questions/15555750/android-intent-getstringextra-returns-null
        //
        //..... Return the Nickname to the Activity that called me
        val intent = Intent()
        intent.putExtra (NICKNAME_KEY, sNickname.toString())
        setResult (REQUEST_CODE_GET_NICKNAME, intent)
        finish ()

    }

    fun showAllSessions () {

        //..... Get the Internet bulletin board info
        getSessionInfo ()
    }

    fun getSessionInfo () {

    }
}