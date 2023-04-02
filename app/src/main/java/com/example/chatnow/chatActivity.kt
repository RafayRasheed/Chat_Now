package com.example.chatnow
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.link.view.*
import java.text.SimpleDateFormat
import java.util.*


class chatActivity : AppCompatActivity(), control_top_bar {

    lateinit var msgRecylerView: RecyclerView
    lateinit var msgAdapter: messageAdapter
    private var msgList: ArrayList<messageInfo> = ArrayList()
    private var groupMembers = hashMapOf<String, String>()
    private lateinit var mdbRef: DatabaseReference
    private var online:Boolean = false
    private var lastOnline:String = ""
    private lateinit var status:TextView

    //Person Information
     var personUserID:String = ""
     var personName:String = ""
     var personImage:String = ""
     var personUid:String = ""
     var personEmail:String = ""
     var myName:String = ""


    var currentStatus: String = ""
    val userUid = FirebaseAuth.getInstance().currentUser?.uid.toString()
    var youBlock: Boolean = false
    var youWereB: Boolean = false
    var unread: Int = 0
    var sendImageURL: String? = null



    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        blockText.isVisible = false
        chatImageVideo.isVisible = false
        status = findViewById(R.id.userStatus)

        msgRecylerView =  findViewById(R.id.messageRecyclerView)
        getPersonInformatin()
        mdbRef = FirebaseDatabase.getInstance().reference

        msgAdapter = messageAdapter(this, this, chat() ,personUid ,msgList)
        msgRecylerView.layoutManager = LinearLayoutManager(this)
        msgRecylerView.adapter = msgAdapter
//        personNameTxt.text = personName
        if(personUserID != ""){
            mdbRef.child("user").child(personUid).addValueEventListener(object :ValueEventListener{
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val s = snapshot.child("lastOnline").value.toString()
                        val personInfo = snapshot.getValue(userInfo::class.java)!!
                        online = personInfo.status == "online"
                        if (lastOnline == "") lastOnline = datetoo(s.toLongOrNull())
                        if (!online){
//                            Log.d("okbro",online.toString())
                            lastOnline = datetoo(s.toLongOrNull())
                            setOfflineStatus()
                        }
                        else {
                            status.text = "online"
                        }

                        personName = personInfo.name.toString()
                        personNameTxt.text = personName
                        if(personImage!=personInfo.image){
                            personImage = personInfo.image.toString()
                            loadImage()
                        }
                        personUserID = personInfo.userID.toString()
                        personEmail = personInfo.email.toString()



                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    return
                }
            })
            setAllMessage()
            youWereBlock()
            receiveBlockList()
            showChatStatus()
            date()
            setTypingNull()

        }
        else{
            mdbRef.child("groups").child(personUid).child("info").addValueEventListener(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val groupInfo = snapshot.getValue(userInfo::class.java)!!
                        personName = groupInfo.name.toString()
                        personNameTxt.text = personName
                        if(personImage!=groupInfo.image){
                            personImage = groupInfo.image.toString()
                            loadImage()
                        }

                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    return
                }
            })
            getGroupMembers()
            setAllGroupMessages()
            getGroupStatus()

            mdbRef.child("user").child(userUid).child("name").addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) myName = snapshot.value.toString().trim().split("\\s+".toRegex())[0]
                }

                override fun onCancelled(error: DatabaseError) {
                    return
                }
            })

        }



        sendButton.setOnClickListener {
            val message = messageBox.text.toString()

            if (chatImageVideo.isVisible){
                if(sendImageURL!= null) sendImageVideo(message)
            }
            else if (message == ""){
                sendVoice()
            }
            else{
                sendMessage(message)

            }
        }

        menuChatActivity.setOnClickListener {
            showPopupMenu(topBarChatActivity)
        }
        personProfileImage.setOnClickListener {
            if (chat()) goToProfile() else goToGroupInfo()
        }
        chatBackButton.setOnClickListener{
            onBackPressed()
        }
        messageBox.addTextChangedListener {
            val edt = messageBox.text.toString()
            setTypingStatus(edt)
            if(edt == "") setForOther() else setForMessage()
        }
        link.setOnClickListener {
            showPopupLink()
        }

        selectImageCancel.setOnClickListener {
            chatImageVideo.isVisible = false
            if (messageBox.text.toString() == "") setForOther()
        }
        downArrow.setOnClickListener {
            messageRecyclerView.scrollToPosition(msgAdapter.itemCount - 1)
        }

        call.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            user?.sendEmailVerification()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (user.isEmailVerified) Log.d("Success", "Yes") else Log.d("Success", "hoo")
                } else {
                    Log.i("Success", "No")
                }
            }
        }
        messageRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val myLayoutManager: LinearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
                val scrollPosition = myLayoutManager.findLastVisibleItemPosition()
                allUnread.isVisible = msgAdapter.itemCount-1 != scrollPosition
            }
        })

        if (msgAdapter.itemCount == 0) allUnread.isVisible = false
    }






    private fun datetoo(s: Long?):String{
    //        val sdf = SimpleDateFormat("h:mm a_dd/MM/yyyy")
        val sdf = SimpleDateFormat("h:mm a_dd/MM/yyyy", Locale.ENGLISH)

        if (s != null) {
            return sdf.format(s)
        }
        else return ""
    }

    override fun onBackPressed() {
        setTypingNull()
        super.onBackPressed()
    }

    override fun onPause() {
        setTypingNull()
        super.onPause()
    }

    private fun setForMessage(){
        sendButton.setImageResource(R.drawable.send1)
        link.isVisible = false
    }

    private fun setForOther(){
        link.isVisible = true
        sendButton.setImageResource(R.drawable.voice)
    }


    @SuppressLint("SimpleDateFormat")
    private fun date():String{
        val sdf = SimpleDateFormat("h:mm a_dd/MM/yyyy")
        return sdf.format(Date())
    }

    private fun setTypingStatus(edt: String){
        if (edt == ""){
            setTypingNull()
        }
        else{
            setTyping()
        }
    }

    private fun setTypingNull(){
        if (chat()) {mdbRef.child("chats").child(userUid).child(personUid).child("status").setValue("")}
        else{mdbRef.child("groups").child(personUid).child("chats").child("status").setValue("")}

    }
    private fun setTyping(){

        if (chat()) {mdbRef.child("chats").child(userUid).child(personUid).child("status").setValue("typing...")}
        else if (myName != "") {
            val s = "$userUid $myName"
//            Log.d("okokok", s )

            mdbRef.child("groups").child(personUid).child("chats").child("status").setValue(s)
        }
    }

    private fun chat(): Boolean{
        return intent.getStringExtra("personUserID").toString() != ""
    }

    private fun sendVoice() {
        Toast.makeText(this@chatActivity, "Voice", Toast.LENGTH_SHORT).show()
    }


    @SuppressLint("SimpleDateFormat")
    private fun sendImageVideo(message: String){
        messageBox.setText("")
        chatImageVideo.isVisible = false
        val sdf = SimpleDateFormat("h:mm a")
        val messageObject = messageInfo(message , "image", sendImageURL!! , userUid, date(), "", "", myName)

        if (chat()) {
            val myfi: DatabaseReference =
                mdbRef.child("chats").child(userUid).child(personUid).child("messages").push()
            myfi.setValue(messageObject).addOnSuccessListener {
                mdbRef.child("chats").child(personUid).child(userUid).child("messages")
                    .child(myfi.key.toString())
                    .setValue(messageObject)
            }
        }
        else{
            mdbRef.child("groups").child(personUid).child("chats").child("messages").push().setValue(messageObject)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun date1():String{
//        val sdf = SimpleDateFormat("h:mm a_dd/MM/yyyy")
        val sdf = SimpleDateFormat("yyyyMMddHHmmssSS")

        return sdf.format(Date())
    }


    @SuppressLint("SimpleDateFormat")
    private fun sendMessage(message: String){
        messageBox.setText("")
        val messageObject =
            messageInfo(message, "messages", "", userUid, date(), "", "", myName)
        if(chat()) {
            mdbRef.child("chats").child(userUid).child("order").child(personUid).child("time").setValue(date1().toLongOrNull())
            mdbRef.child("chats").child(personUid).child("order").child(userUid).child("time").setValue(date1().toLongOrNull())
            val myfi: DatabaseReference = mdbRef.child("chats").child(userUid).child(personUid).child("messages").push()
            myfi.setValue(messageObject).addOnSuccessListener {
                mdbRef.child("chats").child(personUid).child(userUid).child("messages")
                    .child(myfi.key.toString()).setValue(messageObject)
            }.addOnSuccessListener {
                if (!chatUnread.isVisible) messageRecyclerView.scrollToPosition(msgAdapter.itemCount-1)
            }
        }
        else{
            mdbRef.child("groups").child(personUid).child("time").setValue(date1().toLongOrNull())
            val myfi: DatabaseReference = mdbRef.child("groups").child(personUid).child("chats").child("messages").push()
                myfi.setValue(messageObject).addOnSuccessListener {
                    groupMembers.mapKeys {
                        if (it.key != userUid )mdbRef.child("groups").child(personUid).child("chats").child("messages").child(myfi.key.toString()).child("readBy").child(it.key).setValue("n")
                    }
                }.addOnSuccessListener {
                    if (!chatUnread.isVisible) messageRecyclerView.scrollToPosition(msgAdapter.itemCount-1)
                }
        }

    }


    var selectedPhotoUri: Uri? = null
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            selectedPhotoUri = data.data
            try {
                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)
                chatImageVideo.isVisible = true
                selectImage.setImageDrawable(null)
                loadingImage.isVisible = true
                uploadImageToFirebaseStorage(bitmap)
            }
            catch (e: Exception){
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImageToFirebaseStorage(bitmap: Bitmap){
        if (selectedPhotoUri == null) return

        val filname: String = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filname")
        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    sendImageURL = it.toString()
                    loadingImage.isVisible = false
                    selectImage.setImageBitmap(bitmap)
                    setForMessage()
                }
            }
//        Toast.makeText(this, i.toString(), Toast.LENGTH_SHORT).show()
    }
    private fun getGroupStatus(){
        mdbRef.child("groups").child(personUid).child("chats").child("status").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists() && snapshot.value!="") {

                    val groupStatusFull =  snapshot.value.toString().trim().split("\\s+".toRegex())
                    currentStatus = if ( groupStatusFull[0]!= userUid) "${groupStatusFull[1]} is typing..." else ""
                    showGroupStatus()

                }
                else{
                    currentStatus = ""
                    showGroupStatus()
                }
            }
            override fun onCancelled(error: DatabaseError) {

                return
            }

        })
    }

    private fun getGroupMembers(){
        mdbRef.child("groups").child(personUid).child("members").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (data in snapshot.children){
                    if (data.exists()) groupMembers[data.key.toString()] = data.value.toString()
                }
                showGroupStatus()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@chatActivity, error.toString(), Toast.LENGTH_SHORT).show()
            }

        })
    }
    private fun setAllGroupMessages(){
        var position = 0
        var unReadBool = false
        mdbRef.child("groups").child(personUid).child("chats").child("messages").addValueEventListener(object : ValueEventListener{
            @SuppressLint("NotifyDataSetChanged", "SimpleDateFormat")
            override fun onDataChange(snapshot: DataSnapshot){
                val sdf = SimpleDateFormat("dd/MM/yyyy")
                val cal = Calendar.getInstance()
                cal.add(Calendar.DATE, -1)
                val yesterdayL: String = sdf.format(cal.time)
                val today: List<String> = date().split("_")
                var last = ""
                msgList.clear()
                unread = 0
                for(postSnapshot in snapshot.children){
                    val allow = postSnapshot.child("notAllow").child(userUid).exists()
                    if (!allow) {
                        val message = postSnapshot.getValue(messageInfo::class.java)
                        if (message != null) {
                            if (message.senderID != null) {
                                val date = message.time.toString()
                                val array: List<String> = date.split("_")
                                val tareekh = if (array[1] == today[1]) "Today" else if (array[1] == yesterdayL) "Yesterday" else array[1]

                                if (tareekh != last){
                                    last = tareekh
                                    val groupmessage = messageInfo(last, null, null, null, null, null, null, null)
                                    msgList.add(groupmessage)
                                }
                                message.messageKey = postSnapshot.key.toString()
                                message.time = array[0]
                                msgList.add(message)
                                val readBy = postSnapshot.child("readBy").child(userUid)
                                if (readBy.exists() && readBy.value == "n") {
                                    unread++
                                    if(!unReadBool){
                                        position = msgAdapter.itemCount - 1
                                        if (position >= 2) position -= 2
                                        unReadBool = true
                                        chatUnread.isVisible = true
                                        messageRecyclerView.scrollToPosition(position)
                                    }
                                    chatUnread.text = unread.toString()
                                }

                            }
                        }
                    }
                }
                msgAdapter.notifyDataSetChanged()
                if(unread != 0 && !chatUnread.isVisible){
                    chatUnread.isVisible = true
                }

                if(unread == 0 && chatUnread.isVisible){
                    chatUnread.isVisible = false
                }
                if(!unReadBool){
                    position  = msgAdapter.itemCount - 1
                    messageRecyclerView.scrollToPosition(position)
                    unReadBool = true
                }
                else if(!chatUnread.isVisible)messageRecyclerView.scrollToPosition(msgAdapter.itemCount - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@chatActivity, error.toString(), Toast.LENGTH_SHORT).show()
            }

        })
    }
    private fun setAllMessage(){
        var position = 0
        var unReadBool = false

        mdbRef.child("chats").child(userUid).child(personUid).child("messages").addValueEventListener(object : ValueEventListener{
            @SuppressLint("NotifyDataSetChanged", "SimpleDateFormat")
            override fun onDataChange(snapshot: DataSnapshot) {

                val sdf = SimpleDateFormat("dd/MM/yyyy")
                val cal = Calendar.getInstance()
                cal.add(Calendar.DATE, -1)
                val yesterdayL: String = sdf.format(cal.time)
                val today: List<String> = date().split("_")
                var last = ""
                msgList.clear()
                unread = 0
                for(postSnapshot in snapshot.children){
                    val message= postSnapshot.getValue(messageInfo::class.java)
                    if (message != null) {
                        if (message.senderID != null) {
                            val date = message.time.toString()
                            val array: List<String> = date.split("_")
                            val tareekh = if (array[1] == today[1]) "Today" else if (array[1] == yesterdayL) "Yesterday" else array[1]
                            if (tareekh != last) {
                                last = tareekh
                                val groupmessage =
                                    messageInfo(last, null, null, null, null, null, null, null)
                                msgList.add(groupmessage)
                            }
                            message.messageKey = postSnapshot.key.toString()
                            message.time = array[0]
                            msgList.add(message)

                            if (message.senderID != userUid ){
                                if (message.read != "yes"){
                                    unread++
                                    if(!unReadBool){
                                        position = msgAdapter.itemCount - 1
                                        if (position >= 2) position -= 2
                                        unReadBool = true
                                        chatUnread.isVisible = true
                                        messageRecyclerView.scrollToPosition(position)
                                    }
                                    chatUnread.text = unread.toString()

                                }

                            }

                        }
                    }
                }
                msgAdapter.notifyDataSetChanged()
                if(unread != 0 && !chatUnread.isVisible){
                    chatUnread.isVisible = true
                }

                if(unread == 0 && chatUnread.isVisible){
                    chatUnread.isVisible = false
                }

                if(!unReadBool){
                    messageRecyclerView.scrollToPosition(msgAdapter.itemCount - 1)
                    unReadBool = true
                }
                else if(!chatUnread.isVisible)messageRecyclerView.scrollToPosition(msgAdapter.itemCount - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@chatActivity, error.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showGroupStatus(){
        if(currentStatus != "" ){
            userStatus.text = currentStatus
        }
        else if(groupMembers.size!= 0){
            val s = "You & ${groupMembers.size - 1} others"
            userStatus.text = s
        }
        else{
            userStatus.text = ""
        }
    }

    private fun showChatStatus() {
        mdbRef.child("chats").child(personUid).child(userUid).child("status").addValueEventListener(object : ValueEventListener{
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.value == "typing..." && online)status.text = "typing..."
                else if (online) status.text = "online"
                else setOfflineStatus()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@chatActivity, error.toString(),Toast.LENGTH_SHORT).show()
            }
        })
    }

    @SuppressLint("SimpleDateFormat")
    private fun setOfflineStatus(){
        val sdf = SimpleDateFormat("dd/MM/yyyy")
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        val yesterdayL: String = sdf.format(cal.time)
        val array: List<String> = lastOnline.split("_")
        val today: List<String> = date().split("_")
        status.text = if (array[1] == today[1]) "last seen "+ array[0] else if(array[1] == yesterdayL) "last seen yesterday" else "last active "+ array[1]

    }



    private fun receiveBlockList(){
        mdbRef.child("block").child(userUid).child(personUid).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    youBlock = true
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@chatActivity, error.toString(),Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun youWereBlock(){

        mdbRef.child("block").child(personUid).child(userUid).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    youWereB = true
                    doBlockUnblock(youWereB)
                }
                else{
                    youWereB = false
                    doBlockUnblock(youWereB)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@chatActivity, error.toString(),Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun doBlockUnblock(block: Boolean){
        if (block){
            edtParent.isVisible = false
            sendButton.isVisible = false
            blockText.isVisible = true
//            loadImage("")
        }

        else{
//            loadImage("something")
            edtParent.isVisible = true
            sendButton.isVisible = true
            blockText.isVisible= false
        }
    }

    private fun checkBlock(): Boolean{
       return youBlock
    }

    private fun goToProfile(){
        val intent = Intent(this, user_profile::class.java)
        intent.putExtra("personName", personName)
        intent.putExtra("personUserID", personUserID)
        intent.putExtra("personImage", personImage)
        intent.putExtra("personEmail", personEmail)
        intent.putExtra("myProfile", "False")
        startActivity(intent)
    }
    private fun goToGroupInfo(){
        val intent = Intent(this, profile_group::class.java)
        intent.putExtra("groupUid", personUid)
        startActivity(intent)
    }

    private fun clearChat(){
        if (chat()) {
            mdbRef.child("chats").child(userUid).child(personUid).child("messages").setValue(null)
        }
        else{
            val path: DatabaseReference = mdbRef.child("groups").child(personUid).child("chats").child("messages")
            path.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (s in snapshot.children){
                        val notAllow = s.child("notAllow").child(userUid).exists()

                        if (!notAllow) path.child(s.key.toString()).child("notAllow").child(userUid).setValue(true)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    return
                }
            })
        }

    }

    private fun block(){
        mdbRef.child("block").child(userUid).child(personUid).setValue(true)
    }
    private fun unBlock(){
        mdbRef.child("block").child(userUid).child(personUid).setValue(null)
        youBlock = false
    }

    private fun showPopupMenu(view: View) {

        val popup = PopupMenu(this, view)
        val inflater = popup.menuInflater
        inflater?.inflate(R.menu.menu_chat_activity, popup.menu)
        val menu: Menu = popup.menu
        val profile = menu.findItem(R.id.profile)
        val clearChat = menu.findItem(R.id.clear)
        val block = menu.findItem(R.id.block)


        if (!chat()) {
            profile.title = "Group Info"
            block.title = "Leave"
        }
        else{
            profile.title = "Profile"
            block.title = if (checkBlock()) "UnBlock" else "Block"
        }

        popup.setOnMenuItemClickListener { item: MenuItem? ->

            when (item!!.itemId) {
                R.id.profile -> {
                    if (chat()) goToProfile() else goToGroupInfo()
                }
                R.id.clear -> {
                    clearChat()
                }
                R.id.block -> {
                    if(!checkBlock()){
                        block()
                    }
                    else{
                        unBlock()
                    }
                }
            }
            true
        }
        popup.show()
    }

    fun getNavigationBarHeight(activity: Activity): Int {
        val rectangle = Rect()
        val displayMetrics = DisplayMetrics()
        activity.window.decorView.getWindowVisibleDisplayFrame(rectangle)
        activity.windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return displayMetrics.heightPixels - (rectangle.top + rectangle.height())
    }

    private fun showPopupLink() {

        val popup = PopupWindow(this)
        val vie= layoutInflater.inflate(R.layout.link,null)
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.contentView = vie
        popup.isOutsideTouchable = true
        popup.showAtLocation(link,Gravity.BOTTOM, 0 , getNavigationBarHeight(this)+edtChatActivity.height+2 )

        popup.setOnDismissListener {
            Handler().postDelayed({
                popup.isOutsideTouchable = false
            }, 300)
        }

        link.setOnClickListener {
            if(!popup.isOutsideTouchable){
                showPopupLink()
            }
        }

        vie.linkDoc.setOnClickListener {
            popup.dismiss()
            selelecDoc()
        }

        vie.linkImage.setOnClickListener {
            popup.dismiss()
            selelecImage()
        }

        vie.linkVideo.setOnClickListener {
            popup.dismiss()
            selelecVideo()
        }

    }
//    private fun hidelink(){
//        frameLayout.isVisible = false
//
//    }

    private fun selelecDoc(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "*/*"
        startActivityForResult(intent, 0)
    }

    private fun selelecImage(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 0)
    }
    private fun selelecVideo(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "video/*"
        startActivityForResult(intent, 0)
    }


    private fun loadImage() {
        if (personImage!=""){
            Glide.with(applicationContext)
                .asBitmap()
                .load(personImage).centerCrop().fitCenter()
                .into(object : CustomTarget<Bitmap>(){
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        personProfileImage.setImageBitmap(resource)
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
        }
        else{
            personProfileImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.profile))
        }
    }



    private fun getPersonInformatin(){
        personUid = intent.getStringExtra("personUid").toString()
        personUserID = intent.getStringExtra("personUserID").toString()
    }

    override fun off() {
        chatActivityBar.isVisible = false

    }

    override fun on() {
        chatActivityBar.isVisible = true

    }


}