package com.example.chatter1

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.preference.PreferenceManager

const val NICKNAME_KEY = "Nickname"
const val NICKNAME_REQUEST_CODE = 1234

class MainActivity : AppCompatActivity() {

    lateinit var m_sNickname : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //..... Get m_sNickname from the device
        m_sNickname = getNicknameFromDevice()
        //..... If the Nickname does not exist, have to ask the user
        if (m_sNickname.isEmpty()) {
            val intent = Intent(this, NicknameActivity::class.java)
            startActivityForResult(intent, NICKNAME_REQUEST_CODE)
        } else {
            //..... Start the next process
            addNicknameToTitleBar()
        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menu_edit_nickname -> {
                //Start NicknameActivity to change the nickname
                val intent = Intent(this, NicknameActivity::class.java)
                intent.putExtra (NICKNAME_KEY, m_sNickname.toString())
                startActivityForResult(intent, NICKNAME_REQUEST_CODE)
            }
        }

        return super.onOptionsItemSelected(item)
    }

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

    fun savePreferenceData (sData: String) {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = pref.edit()
        editor.putString(NICKNAME_KEY, sData)
            .apply()
   }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //..... 2021/03/20: The following codes now work after inserting .toString()
        //      to the intent.puExtra () in NicknameActivity.onClickButtonOK as follows:
        //          intent.putExtra (NICKNAME_KEY, sNickname.toString())
        //      This solution was found in Stack Overflow. The URL is below
        //          https://stackoverflow.com/questions/15555750/android-intent-getstringextra-returns-null
        val sNickname = data?.getStringExtra (NICKNAME_KEY)
        if (!sNickname.isNullOrEmpty()) {
            m_sNickname = sNickname
            //..... Write this information to the device memory
            savePreferenceData (m_sNickname)
            //..... Now start the next process
            addNicknameToTitleBar()
        }

    }

    fun addNicknameToTitleBar () {

        val sTitle = "Chatter " + m_sNickname
        setTitle(sTitle)
    }

}