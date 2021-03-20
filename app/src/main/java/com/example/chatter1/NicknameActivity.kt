package com.example.chatter1

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.text.trimmedLength

const val NICKNAME_MAX_SIZE = 12

//const val RETURN_CODE_NICKNAME = 1234
//const val RETURN_DATA_KEY = "Nickname"

class NicknameActivity : AppCompatActivity() {

    lateinit var m_textError : TextView
    lateinit var m_editNickname : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nickname)

        m_textError     = findViewById(R.id.textViewError)
        m_editNickname  = findViewById(R.id.editNickname)
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
        setResult (NICKNAME_REQUEST_CODE, intent)
        finish ()

    }
}