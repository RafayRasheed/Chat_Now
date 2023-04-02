package com.example.chatnow

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.MenuRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.android.synthetic.main.userlist_layout.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


class userAdapter(
    val listner: control_top_bar?, val context: Context, var userList: ArrayList<userInfo>, val unread: HashMap<String, Int>?, val lastMsg: HashMap<String, messageInfo?>?,
    val typing: HashMap<String, String>?, val dialog: Dialog, val shareMode: Boolean, val shareMsg: ArrayList<messageInfo>?, val persond: HashMap<String, String?>?):

    RecyclerView.Adapter<userAdapter.userViewHolder>() {
    lateinit var viewGroup: ViewGroup
    lateinit var view: View
    var selected: ArrayList<userInfo> = ArrayList()
    val userUid = FirebaseAuth.getInstance().currentUser?.uid.toString()
    val primaryActionModeCallback = PrimaryActionModeCallback()
    val mDbRef:DatabaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): userViewHolder {
        viewGroup = parent
        view = LayoutInflater.from(context).inflate(R.layout.userlist_layout, parent , false)

        return userViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "ResourceAsColor", "SimpleDateFormat", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: userViewHolder, position: Int) {

        val currentUser = userList[position]
        val unRead = unread?.get(currentUser.uid)
        val lastM = lastMsg?.get(currentUser.uid)
        val groupPerson = persond?.get(currentUser.uid)


        if(currentUser.image != ""){
            val image1 = currentUser.image
            Glide.with(context)
                .asBitmap()
                .load(image1)
                .into(object : CustomTarget<Bitmap>(){
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        holder.itemView.profile_image.setImageBitmap(resource)

                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })

        }
        else{
            holder.itemView.profile_image.setImageResource(R.drawable.profile1)
        }

        holder.text_name.text= currentUser.name
        holder.statusOnline.isVisible = currentUser.status == "online"

        if (!dialog.isShowing ){
            if (currentUser in selected){
                holder.all_user.setBackgroundColor(Color.LTGRAY)
            }
            else{
                holder.all_user.setBackgroundColor(Color.parseColor("#FAFAFA"))
            }
            if (lastM!=null){
                holder.lastMsgTime.text = lastM.time
                if (lastM.senderID!=userUid && lastM.read != "yes"){
                    setLastMessage( holder.text_userID, lastM, Color.BLACK, Typeface.BOLD, groupPerson)

                }
                else{
                    setLastMessage( holder.text_userID, lastM, Color.DKGRAY, Typeface.NORMAL, groupPerson)
                }
            }
            else{
                holder.text_userID.text = ""
                holder.lastMsgTime.text = ""
            }
            val s = typing?.get(currentUser.uid)
            if (s != null) {
                if (s == "typing..." || s.length> 9) {
                    holder.text_userID.text = s
                    holder.text_userID.setTextColor(R.color.barColor)
                    holder.text_userID.setTypeface(null, Typeface.BOLD)

                }
            }
        }
        else{
            holder.text_userID.text= currentUser.userID
            if (shareMode){
                if (currentUser in selected){
                    holder.all_user.setBackgroundColor(Color.LTGRAY)
                }
                else{
                    holder.all_user.setBackgroundColor(Color.parseColor("#FAFAFA"))
                }
            }
        }
//        Toast.makeText(context, unRead.toString(), Toast.LENGTH_SHORT).show()
        if (unRead!= null && unRead != 0) {
            holder.unread.isVisible = true
            holder.unread.text= unRead.toString()
        }
        else{
            holder.unread.isVisible = false
        }

        holder.itemView.setOnLongClickListener {

            if (currentUser.userID !=null){
                if (primaryActionModeCallback.mode == null && !dialog.isShowing){
                    primaryActionModeCallback.startActionMode(R.menu.menu_long_click_main)
                    if (primaryActionModeCallback.mode != null){
                        selected.add(currentUser)
                        notifyItemChanged(position)
                    }
                }
            }
            else{
                Toast.makeText(context, "You Cant Delete Group", Toast.LENGTH_SHORT).show()
            }
            return@setOnLongClickListener true
        }

        holder.itemView.setOnClickListener {

            if (primaryActionModeCallback.mode != null){
                if (currentUser in selected) {
//                    holder.all_user.setBackgroundColor(Color.WHITE)
                    selected.remove(currentUser)
                    notifyItemChanged(position)
                    if (selected.isEmpty()){
                        primaryActionModeCallback.finishActionMode()
                    }

                }
                else if (currentUser.userID !=null){
                    selected.add(currentUser)
                    notifyItemChanged(position)
//                    holder.all_user.setBackgroundColor(Color.LTGRAY)
                }
                else{
                    Toast.makeText(context, "You Cant Delete Group", Toast.LENGTH_SHORT).show()
                }
            }
            else if(!shareMode){
                val intent = Intent(context, chatActivity::class.java)

                if(currentUser.userID != null ){
                    intent.putExtra("personUid", currentUser.uid)
                    intent.putExtra("personUserID", currentUser.userID)
                }
                else{
                    intent.putExtra("personUid", currentUser.uid)
                    intent.putExtra("personName", currentUser.name)
                    intent.putExtra("personImage", currentUser.image)
                    intent.putExtra("personUserID", "")
                    intent.putExtra("personEmail", "")
                }

//
//
                context.startActivity(intent)
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
            else{
                val shareBut = dialog.findViewById<ImageView>(R.id.share_msg_button)
                val create_group = dialog.findViewById<RelativeLayout>(R.id.create_Group)
                val group_profile = dialog.findViewById<ImageView>(R.id.group_image)
                val group_name = dialog.findViewById<EditText>(R.id.group_name)
                val group_members = dialog.findViewById<TextView>(R.id.group_members)
//                Log.d("sharemm", holder.shareBut.toString())

                if (currentUser in selected){
                    selected.remove(currentUser)
                    if(selected.isEmpty()) shareBut.isVisible = false
                }
                else{
                    if(selected.isEmpty()) shareBut.isVisible = true
                    selected.add(currentUser)

                }
                notifyItemChanged(position)
                if (create_group.isVisible){
                    var s = "Members: You"
                    selected.forEach {
                        s = "$s, ${it.name?.let { it1 -> fierstName(it1) }}"
                    }
                    group_members.text = s
                }

                group_profile.setOnClickListener {

                }

                shareBut.setOnClickListener {

                    if (create_group.isVisible) {
                        var dialog12: AlertDialog?
                        val builder = AlertDialog.Builder(context)
                        val view = LayoutInflater.from(context).inflate(R.layout.fragment_loading, null)
                        builder.setView(view)
                        dialog12 = builder.create()
                        dialog12.window?.attributes?.windowAnimations =  R.style.DialogTheme
                        val window: Window? = dialog12!!.window
                        window?.setGravity(Gravity.CENTER)
                        dialog12.show()

                        val s = group_name.text.toString()
                        if (s != ""){
                            val uid = UUID.randomUUID().toString()
                            mDbRef.child("groups").child(uid).child("info").setValue(userInfo(uid, s, null, null, "", null))
                                .addOnCompleteListener {
                                    selected.forEach {
                                        mDbRef.child("groups").child(uid).child("members").child(it.uid.toString()).setValue("n")
                                        mDbRef.child("chats").child(it.uid.toString()).child("order").child(uid).child("group").setValue(true)
                                        mDbRef.child("chats").child(it.uid.toString()).child("order").child(uid).child("time").setValue(date1().toLongOrNull())

                                    }
                                    mDbRef.child("groups").child(uid).child("members").child(userUid).setValue("y")
                                    mDbRef.child("chats").child(userUid).child("order").child(uid).child("group").setValue(true)
                                    mDbRef.child("chats").child(userUid).child("order").child(uid).child("time").setValue(date1().toLongOrNull())
                                    mDbRef.child("groups").child(uid).child("chats").child("status").setValue("")


                                }.addOnCompleteListener {
                                    selected.clear()
                                    dialog.dismiss()
                                    group_name.setText("")
                                    group_members.text = "Members: You"
                                    shareBut.isVisible = false
                                    val intent = Intent(context, chatActivity::class.java)
                                    intent.putExtra("personUid", uid)
                                    intent.putExtra("personName", s)
                                    intent.putExtra("personUserID", "")
                                    intent.putExtra("personImage", "")
                                    intent.putExtra("personEmail", "")

                                    dialog12.dismiss()
                                    context.startActivity(intent)

                                }
                        }
                        else{
                            Toast.makeText(context, "Enter Group Name", Toast.LENGTH_LONG).show()
                        }
                    }

                    else{
                        shareMsg?.forEach {
                            val sdf = SimpleDateFormat("h:mm a_dd/MM/yyyy")
                            val msg = it
                            Log.d("sharem", msg.messageKey.toString() + "oooooooooooo")

                            setForwardMsg(msg, sdf.format(Date()))

                            selected.forEach { it2 ->
                                if(it2.uid != null ){
                                    sendMessage(msg, it2.uid!!, it2.userID)

                                }

                            }

                        }
                        val s = selected[selected.size - 1]
                        val intent = Intent(context, chatActivity::class.java)
                        intent.putExtra("personUid", s.uid)
                        intent.putExtra("personName", s.name)
                        if (s.userID == null){
                            intent.putExtra("personUserID", "")
                            intent.putExtra("personImage", "")
                            intent.putExtra("personEmail", "")
                        }
                        else{

                            intent.putExtra("personUserID", s.userID)
                            intent.putExtra("personImage", s.image)
                            intent.putExtra("personEmail", s.email)
                        }


                        selected.clear()
                        dialog.dismiss()
                        shareBut.isVisible = false
                        (context as chatActivity).finish()
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
    @SuppressLint("SimpleDateFormat")
    private fun date1():String{
//        val sdf = SimpleDateFormat("h:mm a_dd/MM/yyyy")
        val sdf = SimpleDateFormat("yyyyMMddHHmmssSS")

        return sdf.format(Date())
    }
    private fun fierstName(name: String): String {
        val words = name.trim().split("\\s+".toRegex())[0]
        return words
    }
    private fun setForwardMsg(msg: messageInfo, time: String?){
        msg.time = time
        msg.forward = "yes"
        msg.senderID = userUid
        msg.read = ""
        msg.messageKey = msg.messageKey+ "htf1"
    }
    private fun sendMessage(msg: messageInfo, personUid: String, userID: String?){
        if (userID == null){
            mDbRef.child("groups").child(personUid).child("chats").child("messages").push().setValue(msg)

        }
        else{
            val myfi: DatabaseReference = mDbRef.child("chats").child(userUid).child(personUid).child("messages").push()
            myfi.setValue(msg).addOnSuccessListener {
                mDbRef.child("chats").child(personUid).child(userUid).child("messages").child(myfi.key.toString())
                    .setValue(msg).addOnSuccessListener {
                    }
            }
        }

    }

    private fun setLastMessage(textView: TextView, LastM: messageInfo, color: Int , typeface: Int, groupperson: String?){
        var txt = ""
        when (LastM.type) {
            "image" -> {
                txt = "Photo"
            }
            "video" -> {
                txt = "Video"
            }
            else -> {
                txt = LastM.message.toString()
            }
        }
        if (LastM.senderID == userUid){
            txt = "You: $txt"
        }
        else if( groupperson!= null) txt = "$groupperson: $txt"
        textView.text = txt
        textView.setTextColor(color)
        textView.setTypeface(null, typeface)
    }


    override fun getItemCount(): Int {
        return userList.size
    }
    @SuppressLint("NotifyDataSetChanged")
    fun filerList(filter: ArrayList<userInfo> ){
        userList = filter
        notifyDataSetChanged()
    }


    inner class PrimaryActionModeCallback : ActionMode.Callback {

        var mode: ActionMode? = null
        @MenuRes private var menuResId: Int = 0

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            this.mode = mode
            mode.menuInflater.inflate(menuResId, menu)
            listner?.off()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onDestroyActionMode(mode: ActionMode) {
            this.mode = null
            selected.clear()
            android.os.Handler().postDelayed({
                listner?.on()
            }, 200)
            notifyDataSetChanged()
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
//            onActionItemClickListener?.onActionItemClick(item)
            if(item.itemId == R.id.delete_user){
                AlertDialog.Builder(context)
                .setMessage("are you sure to delete")
                .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                    selected.forEach {
                        mDbRef.child("chats").child(userUid).child("order").child(it.uid.toString()).setValue(null)
                        mDbRef.child("chats").child(userUid).child(it.uid.toString()).setValue(null)
                    }
                    finishActionMode()
                }).setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
                    return@OnClickListener
                }).show()

            }

//            mode.finish()
            return true
        }

        fun startActionMode(@MenuRes menuResId: Int) {
            this.menuResId = menuResId
            viewGroup.startActionMode(this)


        }

        fun finishActionMode() {
            mode?.finish()
        }
    }

    class userViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val text_name = itemView.findViewById<TextView>(R.id.txt_name)
        val text_userID = itemView.findViewById<TextView>(R.id.txt_userID)
        val statusOnline = itemView.findViewById<ImageView>(R.id.online)
        val unread = itemView.findViewById<TextView>(R.id.unreadMsg)
        val lastMsgTime = itemView.findViewById<TextView>(R.id.timeLastMessage)
        val all_user = itemView.findViewById<RelativeLayout>(R.id.user_layout_full)
    }


}
