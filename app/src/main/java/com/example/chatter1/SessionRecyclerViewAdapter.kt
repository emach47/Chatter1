package com.example.chatter1

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class SessionRecyclerViewAdapter (sessionRecords: MutableList<SessionRecord>) : RecyclerView.Adapter<SessionViewHolder>() {

    //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
    //xxxxx I get a warning message: sessionRecords never referenced.
    //xxxxx Resolve this warning message before committing
    //xxxxx Investigate the difference between sessionTable and sessionRecord
    //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

    //..... Link to NetViewModel
    //val netViewModel : NetViewModel by netViewModel()
    var netViewModel = NetViewModel()
    //val netViewModel = ViewModelProvider(this).get(NetViewModel::class.java)

    //..... Link to SessionActivity
    //val sessionActivity = SessionActivity()

    //var m_sessionTable = MutableList(10) {SessionRecord()}
    var m_iSessions = 0
    var m_sessionTable = sessionRecords
    var m_iTotalSessions = 0

    /**
     * Called when RecyclerView needs a new [ViewHolder] of the given type to represent
     * an item.
     *
     *
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     *
     *
     * The new ViewHolder will be used to display items of the adapter using
     * [.onBindViewHolder]. Since it will be re-used to display
     * different items in the data set, it is a good idea to cache references to sub views of
     * the View to avoid unnecessary [View.findViewById] calls.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     *
     * @return A new ViewHolder that holds a View of the given view type.
     * @see .getItemViewType
     * @see .onBindViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        //..... Link session_one.xml to SessionViewHolder
        val view = LayoutInflater.from(parent.context).inflate (R.layout.session_one, parent, false)
        val sessionViewholder = SessionViewHolder (view)

        //netViewModel = ViewModelProvider.of(NetViewModel)

        //var iSessions = netViewModel.getSussionCount()

        //..... 2021/04/19: The following data all contains zeroes\
        //      sessionActivity.m_iSessions
        //      netViewModel.m_iSessions
        //      However, m_ssesionTable[] contains valid data
        //      So, if I can find out how to pass m_iSessions from SessionActivity,
        //      I may be able to do what I want to do without using Realm.
        //val sessionActivity = SessionActivity()
        //val iSessions = sessionActivity.m_iSessions
        m_iSessions = netViewModel.m_iSessions
        m_iSessions = 2
        //m_iSessions = NetViewModel.m_iSessions
        //m_sessionTable = netViewModel.m_sessionTable


        return sessionViewholder
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the [ViewHolder.itemView] to reflect the item at the given
     * position.
     *
     *
     * Note that unlike [android.widget.ListView], RecyclerView will not call this method
     * again if the position of the item changes in the data set unless the item itself is
     * invalidated or the new position cannot be determined. For this reason, you should only
     * use the `position` parameter while acquiring the related data item inside
     * this method and should not keep a copy of it. If you need the position of an item later
     * on (e.g. in a click listener), use [ViewHolder.getAdapterPosition] which will
     * have the updated adapter position.
     *
     * Override [.onBindViewHolder] instead if Adapter can
     * handle efficient partial bind.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        //val sessionRecord = netViewModel.m_sessionTable[position]

        val sessionRecord = m_sessionTable[position]

        //val iSessions = netViewModel.m_iSessions
        val iSessions = m_iSessions
        //..... Create the view depending on iSessions and position
        if (position <= iSessions)
        {
            //..... This session is in progress and we have the data
            //holder.textSessionGroup?.text  = sessionRecord.sessionGroupId.toString()
            holder.textSessionGroup?.text  = (position + 1).toString()
            holder.textSessionMembers?.text = sessionRecord.sessionMembers.toString()
            holder.textSessionHostName?.text = sessionRecord.sessionHostName
            //..... Set up Guest Names
            val iGuestCount = sessionRecord.sessionMembers - 1
            for (i in 0 until MAX_GUESTS) {
                if (i > iGuestCount) {
                    holder.textSessionGuestName[i]?.text = " "
                }
                else {
                    holder.textSessionGuestName[i]?.text = sessionRecord.sessionGuestName[i]
                }
            }

            if (sessionRecord.sessionMembers  == 0) {
                //holder.buttonSessionAction?.setText(R.string.button_text_start)
                //..... 2021/05/08: This was the only way I could access the value in strings.xml
                //      I got the answer from the following Stack Overflow URL
                //      https://stackoverflow.com/questions/52765121/how-do-i-call-getstring-inside-the-onbindviewholder-method-of-a-recycler-vie
                var sText = holder.itemView.getContext().getString(R.string.button_text_start)
                holder.buttonSessionQuit?.setVisibility (View.VISIBLE)
                //var sText = R.string.button_text_start.toString()
                sText = sText + " " + (position + 1).toString()
                holder.buttonSessionAction?.setText (sText)
            } else
            if (sessionRecord.sessionMembers < MAX_MEMBERS)
            {
                //holder.buttonSessionAction?.setText(R.string.button_text_join)
                var sText = holder.itemView.getContext().getString(R.string.button_text_join)
                sText = sText + " " + (position + 1).toString()
                holder.buttonSessionAction?.setText (sText)
            }
            else {
                holder.buttonSessionAction?.setText(R.string.button_text_full)
            }

        }

        //..... NOTE: Initially no view is displayed. Not even the buttonAction.
        val lightGreen = Color.parseColor("#8888FF88")
        holder.itemView.setBackgroundColor(if (position % 2 == 0) lightGreen else Color.WHITE)

    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        //..... Both result in zero
        //      netViewModel.m_iSessions
        //      m_iSessions
        //val iSessions = netViewModel.m_iSessions + 1
        //val iSessions = m_iSessions + 1
        val iSessions = m_sessionTable.size
        m_iSessions = iSessions
        return iSessions
    }
}