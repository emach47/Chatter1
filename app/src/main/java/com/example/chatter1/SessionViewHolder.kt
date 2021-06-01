package com.example.chatter1

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

//..... 2021/03/27: The following statement causes an error on android if (1) below is not done
import kotlinx.android.synthetic.main.session_one.view.*

class SessionViewHolder (itemView: View): RecyclerView.ViewHolder (itemView) {

    var textSessionGroup: TextView? = null
    var textSessionMembers: TextView? = null
    var textSessionHostName: TextView? = null
    var textSessionGuestName = Array<TextView?>(MAX_GUESTS) {null}
    var buttonSessionAction: Button? = null

    init {

        //The error on the following statements corrected by doing 2 things.
        //(1) Adding 'Kotlin-android-extension' in build gradle (app) even though I got deplicated warning.
        //(2) Adding import kotlinx.android.synthetic.main.session_one.view.*
        textSessionGroup = itemView.textSessionGroup
        textSessionMembers = itemView.textSessionMembers
        textSessionHostName = itemView.textSessionHostName
        textSessionGuestName[0] = itemView.textSessionGuestName1
        textSessionGuestName[1] = itemView.textSessionGuestName2
        textSessionGuestName[2] = itemView.textSessionGuestName3
        buttonSessionAction = itemView.buttonSessionAction
    }
}
