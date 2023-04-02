package com.example.chatnow
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.TimeUtils
import android.view.*
import android.widget.*
import androidx.annotation.MenuRes
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.all_users_list.view.*
import java.util.concurrent.TimeUnit
import java.util.logging.Handler


class messageAdapter(val listener: control_top_bar, val context: Context, val chat: Boolean, val receiverR: String ,val messageList: ArrayList<messageInfo>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val you: Int = 1
    val other: Int = 0
    val group: Int = 2


    var selected: ArrayList<messageInfo> = ArrayList()
    lateinit var viewGroup: ViewGroup
    val primaryActionModeCallback  = PrimaryActionMode()
    val mdbRef: DatabaseReference = FirebaseDatabase.getInstance().reference
    val userUid = FirebaseAuth.getInstance().currentUser?.uid.toString()
    val alluserlist: ArrayList<userInfo> = ArrayList()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        viewGroup = parent
        if (viewType == 1){
            val view: View = LayoutInflater.from(context).inflate(R.layout.send_message, parent, false)
            return sentViewHolder(view)
        }
        else if(viewType == 0){
            val view: View = LayoutInflater.from(context).inflate(R.layout.recieve_message, parent, false)
            return receiveViewHolder(view)
        }
        else{
            val view: View = LayoutInflater.from(context).inflate(R.layout.group_info, parent, false)
            return groupmessage(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messageList[position]
        if (holder.javaClass == groupmessage::class.java){
            val viewHolder = holder as groupmessage
            viewHolder.groupmsg.text = currentMessage.message.toString()
        }
        if(holder.javaClass == sentViewHolder::class.java){
            val viewHolder = holder as sentViewHolder
            if (currentMessage.read == "yes") viewHolder.readMessage.setTextColor(Color.BLUE) else viewHolder.readMessage.setTextColor(Color.DKGRAY)
            viewHolder.senderMessageForward.isVisible = currentMessage.forward == "yes"
            if(currentMessage in selected) viewHolder.sendLayout.setBackgroundColor(Color.parseColor("#8668D6")) else viewHolder.sendLayout.setBackgroundColor(Color.TRANSPARENT)
            setAlLMessages(currentMessage, viewHolder.senderImageVideo, viewHolder.senderImage, viewHolder.sentMessage, viewHolder.sentMessageTime)
            if (!chat){
                mdbRef.child("groups").child(receiverR).child("chats").child("messages").child(currentMessage.messageKey.toString()).child("readBy").addValueEventListener(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var count = 0
                        if (snapshot.value != "all" && snapshot.exists() ){
                            for (data in snapshot.children){
                                if (data.exists() && data.value == "n") {
                                    count++
                                }
                            }
                            if(count == 0 ){
                                mdbRef.child("groups").child(receiverR).child("chats").child("messages")
                                    .child(currentMessage.messageKey.toString()).child("readBy").setValue("all").addOnSuccessListener {
                                        mdbRef.child("groups").child(receiverR).child("chats").child("messages")
                                            .child(currentMessage.messageKey.toString()).child("read").setValue("yes")
                                    }
                            }
                        }

                    }

                    override fun onCancelled(error: DatabaseError) {
                       return
                    }

                })
            }
        }
        else if(holder.javaClass == receiveViewHolder::class.java){
            val viewHolder = holder as receiveViewHolder
            if(currentMessage in selected) viewHolder.receiveLayout.setBackgroundColor(Color.LTGRAY) else viewHolder.receiveLayout.setBackgroundColor(Color.TRANSPARENT)
            viewHolder.receiveMessageForward.isVisible = currentMessage.forward == "yes"
            if (chat) viewHolder.sendBy.isVisible = false
            else{
                viewHolder.sendBy.isVisible = true
                viewHolder.sendBy.text = currentMessage.name
            }

            setAlLMessages(currentMessage, viewHolder.receiverImageVideo, viewHolder.receiveImage, viewHolder.receiveMessage, viewHolder.receiveMessageTime)
            if (chat) {
                mdbRef.child("chats").child(currentMessage.senderID.toString()).child(userUid)
                    .child("messages").child(currentMessage.messageKey.toString())
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) mdbRef.child("chats")
                                .child(currentMessage.senderID.toString()).child(userUid)
                                .child("messages").child(currentMessage.messageKey.toString())
                                .child("read").setValue("yes")
                        }

                        override fun onCancelled(error: DatabaseError) {
                            return
                        }
                    })
                mdbRef.child("chats").child(userUid).child(currentMessage.senderID.toString())
                    .child("messages").child(currentMessage.messageKey.toString())
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) mdbRef.child("chats").child(userUid)
                                .child(currentMessage.senderID.toString()).child("messages")
                                .child(currentMessage.messageKey.toString()).child("read")
                                .setValue("yes")
                        }

                        override fun onCancelled(error: DatabaseError) {
                            return
                        }
                    })
            }
            else{
                mdbRef.child("groups").child(receiverR).child("chats").child("messages").child(currentMessage.messageKey.toString()).child("readBy").addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.value!= "all" && snapshot.exists())
                            mdbRef.child("groups")
                            .child(receiverR).child("chats")
                            .child("messages").child(currentMessage.messageKey.toString())
                            .child("readBy").child(userUid).setValue("y")
                    }

                    override fun onCancelled(error: DatabaseError) {
                        return
                    }

                })
            }

//            Log.d("chCount", a)
        }

        holder.itemView.setOnLongClickListener{
            primaryActionModeCallback.startActionMode(R.menu.menu_long_click_chat)
            if (primaryActionModeCallback.mode !=null){
                selected.add(currentMessage)
                notifyItemChanged(position)

            }
            Log.d("posit", position.toString())
//            AlertDialog.Builder(context)
//                .setMessage("You sure to delete this message")
//                .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
////                    Toast.makeText(context, currentMessage.messageKey.toString(), Toast.LENGTH_SHORT).show()
//                    mdbRef.child("chats").child(userUid).child(receiverR).child("messages").child(currentMessage.messageKey.toString()).setValue(null)
//                }).setNegativeButton("No",DialogInterface.OnClickListener { dialogInterface, i ->
//                    return@OnClickListener
//                }).show()

            return@setOnLongClickListener true
        }
        holder.itemView.setOnClickListener {
            if (primaryActionModeCallback.mode!=null){
                if (currentMessage in selected){
                    selected.remove(currentMessage)
                    if (selected.isEmpty()) {
                        primaryActionModeCallback.finishMode()
                    }
                }
                else selected.add(currentMessage)
                notifyItemChanged(position)

            }
            return@setOnClickListener
        }

    }

    private fun setAlLMessages(currentMessage: messageInfo, ImageVideo: RelativeLayout, Image: ImageView,  Message: TextView, MessageTime: TextView ){
        if(currentMessage.type == "image"){
            ImageVideo.isVisible = true
            Image.setImageDrawable(null)

            setBitmapImage(currentMessage.source, Image)
        }
        else{
            ImageVideo.isVisible = false
        }

        if (currentMessage.message == ""){
            Message.isVisible = false
        }

        else{
            Message.isVisible = true
            Message.text = currentMessage.message
        }

        Image.setOnClickListener {
            val img: Bitmap = (Image.drawable as BitmapDrawable).bitmap
//                Toast.makeText(context, (viewHolder.receiveImage.drawable as BitmapDrawable).bitmap.toString() , Toast.LENGTH_SHORT).show()
            FragShowImage(img)
        }

        MessageTime.text = currentMessage.time
    }


    override fun getItemCount(): Int {
       return messageList.size
    }



    override fun getItemViewType(position: Int): Int {
        val currentMessage = messageList[position]

        if (userUid == currentMessage.senderID){
            return you
        }
        else if (currentMessage.messageKey == null){
            return group
        }
        return other
    }

    private fun setBitmapImage(imageURL: String? , image: ImageView){
        Glide.with(context)
            .asBitmap()
            .load(imageURL)
            .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    image.setImageBitmap(resource)
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }


    private fun FragShowImage(image: Bitmap){

        var dialog: Dialog? = null
        val builder = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen)
        val view = LayoutInflater.from(context).inflate(R.layout.fragmnet_image_show, viewGroup, false);
        val fragProfileImage: ImageView = view.findViewById(R.id.FragProfileImage)
        val fragProfileCancel: ImageView = view.findViewById(R.id.FragProfileCancel)


        builder.setView(view)
        dialog = builder.create()
        dialog.show()
        fragProfileImage.setImageBitmap(image)

        fragProfileCancel.setOnClickListener {
            dialog.cancel()
        }

    }
    private fun allUsersLayout(alluserlist: ArrayList<userInfo>, select: ArrayList<messageInfo>){
        val builderAllUser = AlertDialog.Builder(context, android.R.style.ThemeOverlay_Material_Light)
        val viewAllUser = LayoutInflater.from(context).inflate(R.layout.all_users_list, null)
        builderAllUser.setView(viewAllUser)
        val dialogAllUser = builderAllUser.create()
        dialogAllUser.window?.attributes?.windowAnimations =  R.style.DialogTheme
        dialogAllUser.show()
        val searchBarAllUser: LinearLayout = viewAllUser.findViewById(R.id.search_allUser_bar)
        val edtSearchAllUser: EditText = viewAllUser.findViewById(R.id.edt_search_allUser)

        val back_button = viewAllUser.findViewById<ImageView>(R.id.all_users_back_button)
//        viewAllUser.findViewById<ImageView>(R.id.share_msg_button).isVisible = true
        viewAllUser.findViewById<RelativeLayout>(R.id.create_Group).isVisible = false
        val adapter_all_users = userAdapter(null, context, alluserlist, null, null, null, dialogAllUser, true, select, null)
        viewAllUser.all_users_recyclerView.layoutManager = LinearLayoutManager(context)
        viewAllUser.all_users_recyclerView.adapter = adapter_all_users


        searchBarAllUser.isVisible = true
        back_button.setOnClickListener {
        }
        viewAllUser.back_search_allUser.setOnClickListener {
            dialogAllUser.dismiss()
        }
        edtSearchAllUser.addTextChangedListener {
            val txt: String = edtSearchAllUser.text.toString()
                if (txt != "") filterSearch(txt, adapter_all_users, alluserlist) else adapter_all_users.filerList(alluserlist)
        }
    }
    private fun filterSearch(text: String, fAdapter: userAdapter, list: ArrayList<userInfo>){
        val filtered:ArrayList<userInfo> = ArrayList()
        list.forEach {
            if (it.name!!.lowercase().contains(text.lowercase()) || it.userID!!.lowercase().contains(text.lowercase())){
                filtered.add(it)
            }
        }

        fAdapter.filerList(filtered)
    }


    inner class PrimaryActionMode: ActionMode.Callback{
        var mode:ActionMode? = null
        var length: Long = 0
        var good = false
        @MenuRes private var menuResId: Int = 0



        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            this.mode =mode
            mode?.menuInflater?.inflate(menuResId, menu)

            mdbRef.child("chats").child(userUid).child("order").orderByChild("time").addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    alluserlist.clear()
                    for ((size, snap) in snapshot.children.withIndex()){
                        length =  snapshot.childrenCount

                        if (snap.exists()){
                            val group = snap.child("group").value == true
                            val uid = snap.key.toString()
                            if (!group){
                                mdbRef.child("user").child(uid).addListenerForSingleValueEvent(object : ValueEventListener{
                                    override fun onDataChange(snapshot1: DataSnapshot) {
                                        val user =  snapshot1.getValue(userInfo::class.java)
                                        if (user!= null) alluserlist.add(user)
                                        if (good && size.toLong() == length-1) allUsersLayout(alluserlist, selected)
                                    }
                                    override fun onCancelled(error: DatabaseError) {
                                        return
                                    }
                                })
                            }
                            else{
                                mdbRef.child("groups").child(uid).child("info").addListenerForSingleValueEvent(object : ValueEventListener{
                                    override fun onDataChange(snapshot2: DataSnapshot) {
                                        val user = snapshot2.getValue(userInfo::class.java)
                                        if (user != null) alluserlist.add(user)

                                        if (good && size.toLong() == length - 1){
                                            alluserlist.reverse()
                                            allUsersLayout(alluserlist, selected)

                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        return
                                    }
                                })
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    return
                }
            })
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (item?.itemId == R.id.delete_message){
            AlertDialog.Builder(context)
                .setMessage("You sure to delete this message")
                .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                    selected.forEach {
                        if (chat) {
                            mdbRef.child("chats").child(userUid).child(receiverR).child("messages").child(it.messageKey.toString()).setValue(null)
                        }
                        else{
                            mdbRef.child("groups").child(receiverR).child("chats").child("messages").child(it.messageKey.toString()).child("notAllow").child(userUid).setValue(true)
                        }

                    }
                    mode?.finish()
                    selected.clear()
                }).setNegativeButton("No",DialogInterface.OnClickListener { dialogInterface, i ->
                    return@OnClickListener
                }).show()
            }
            else if (item?.itemId == R.id.share_message){
                if (alluserlist.size.toLong() == length){
                    alluserlist.reverse()
                    allUsersLayout(alluserlist, selected)
                }else good = true

            }

            return true
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onDestroyActionMode(mode: ActionMode?) {
            this.mode = null
            selected.clear()
            android.os.Handler().postDelayed({
                listener.on()
            }, 200)
            notifyDataSetChanged()
        }


        fun startActionMode( @MenuRes menuResId: Int) {
            this.menuResId = menuResId
            viewGroup.startActionMode(this)
            if (mode!=null) listener.off()

        }
        fun finishMode(){
            mode?.finish()
        }


    }
    class sentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val sendLayout = itemView.findViewById<RelativeLayout>(R.id.send_msgLayout)
        val sentMessage = itemView.findViewById<TextView>(R.id.sendMessageTxt)
        val sentMessageTime = itemView.findViewById<TextView>(R.id.sendMessageTime)
        val readMessage = itemView.findViewById<TextView>(R.id.readMessage)
        val senderImageVideo = itemView.findViewById<RelativeLayout>(R.id.sender_image_video)
        val senderImage = itemView.findViewById<ImageView>(R.id.sendImage)
        val senderMessageForward = itemView.findViewById<TextView>(R.id.sendMessageForward)


    }
    class receiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val receiveLayout = itemView.findViewById<RelativeLayout>(R.id.receive_msgLayout)
        val receiveMessageForward = itemView.findViewById<TextView>(R.id.receiveMessageForward)
        val receiveMessage = itemView.findViewById<TextView>(R.id.receiveMessageTxt)
        val receiveMessageTime = itemView.findViewById<TextView>(R.id.receiveMessageTime)
        val receiveImage = itemView.findViewById<ImageView>(R.id.receiveImage)
        val receiverImageVideo = itemView.findViewById<RelativeLayout>(R.id.receiver_image_video)
        val sendBy = itemView.findViewById<TextView>(R.id.sendBy)


    }
    class groupmessage(itemView: View) : RecyclerView.ViewHolder(itemView){

        val groupmsg = itemView.findViewById<TextView>(R.id.groupmessage)
    }


}