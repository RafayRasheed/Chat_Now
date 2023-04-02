package com.example.chatnow

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.back_search
import kotlinx.android.synthetic.main.activity_main.edt_search_users
import kotlinx.android.synthetic.main.all_users_list.view.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() , control_top_bar{
    private lateinit var userRecyclerView: RecyclerView
    private var userlist: ArrayList<userInfo> = ArrayList()
    private var alluserlist: ArrayList<userInfo> = ArrayList()
    private var groupslist: ArrayList<String> = ArrayList()

    private var myChats: ArrayList<String> = ArrayList()

    var s :Boolean = false

    private lateinit var adapter: userAdapter
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    private lateinit var userUid: String
    private var fromLogin: Boolean = false
    private var unread = hashMapOf<String , Int>()
    private var typing = hashMapOf<String , String>()
    private val lastMsg = hashMapOf<String , messageInfo?>()
    private val groupPersonName = hashMapOf<String , String?>()

    private lateinit var mQueryListener: ValueEventListener
    private lateinit var dialogAllUser: Dialog
    private lateinit var dialog: Dialog
    private lateinit var viewAllUser: View

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Firebase.database.setPersistenceEnabled(true)
        fromLogin = intent.getBooleanExtra("fromLogin", false)


        val builder = AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Light)
        val view = layoutInflater.inflate(R.layout.fragment_splash, null)
        builder.setView(view)
        dialog = builder.create()
        dialog.window?.attributes?.windowAnimations =  R.style.DialogTheme
        //style id
        if (!fromLogin) {
            dialog.show()
            loading.isVisible = false
        }
        else {
            recyclerView.isVisible  = false
        }

        mAuth = FirebaseAuth.getInstance()
        userUid = mAuth.currentUser?.uid.toString()

        checkActivity(dialog)
        menuMainActivity.setOnClickListener {
            showPopup(topBarMainActivity)
        }

        val builderAllUser = AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Light)
        viewAllUser = layoutInflater.inflate(R.layout.all_users_list, null)
        builderAllUser.setView(viewAllUser)
        dialogAllUser = builderAllUser.create()
        dialogAllUser.window?.attributes?.windowAnimations =  R.style.DialogTheme
//        val relative:RelativeLayout = view.findViewById(R.id.topBarMainActivity)
        adapter = userAdapter(this, this, userlist, unread, lastMsg, typing , dialogAllUser, false, null, groupPersonName)
        mDbRef = FirebaseDatabase.getInstance().reference
        userUid = mAuth.currentUser?.uid.toString()
        userRecyclerView = findViewById(R.id.recyclerView)
        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userRecyclerView.adapter = adapter


        edt_search_users.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val txt = edt_search_users.text.toString()
                if (txt != "") filterSearch(txt, adapter, userlist) else adapter.filerList(userlist)
            }
        })


        mDbRef.child("chats").child(userUid).child("order").orderByChild("time").addValueEventListener(object :ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    myChats.clear()
                    val all = ArrayList<String>()
                    val myusers = hashMapOf<String , userInfo?>()
                    var i: Long = 0
                    for (snap in snapshot.children){
//                    Log.d("axha1", snap.child("time").value.toString())
                        i++
                        val s = snap.child("group").value
                        val key = snap.key.toString()
                        val last = i==snapshot.childrenCount
                        all.add(key)
                        if (s == null){
                            myChats.add(key)
                            mDbRef.child("user").child(key).addValueEventListener(object : ValueEventListener{
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val user = snapshot.getValue(userInfo::class.java)
                                    if (user != null) myusers[key] = user
                                    if (last) checking(myusers, all)
                                    getChats(key)
                                    getTyping(key)
                                }
                                override fun onCancelled(error: DatabaseError) {
                                    return
                                }
                            })
                            //cha
                        }
                        else{
                            groupslist.add(key)
                            mDbRef.child("groups").child(key).child("info").addValueEventListener(object : ValueEventListener{
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val user = snapshot.getValue(userInfo::class.java)
                                    if (user != null) myusers[key] = user
                                    if (last) checking(myusers, all)
                                    getGroupLastMesg(key)
                                    getGroupTyping(key)
                                }
                                override fun onCancelled(error: DatabaseError) {
                                    return
                                }
                            })
                            //group
                        }
                    }

                }
                else if(dialog.isShowing){
                    dialog.dismiss()
                }

            }

            override fun onCancelled(error: DatabaseError) {
                return
            }

        })


        val presenceRef = Firebase.database.getReference("disconnectmessage")
        presenceRef.onDisconnect().setValue("I disconnected!")
        val database = Firebase.database
        val myConnectionsRef = mDbRef.child("user/$userUid/status")

        val lastOnlineRef = mDbRef.child("/user/$userUid/lastOnline")


        val connectedRef = database.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue<Boolean>() ?: false
                if (connected) {

                    lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP)

                    myConnectionsRef.onDisconnect().setValue("")

                    myConnectionsRef.setValue("online")
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        search_main_activity.setOnClickListener{
            showSearchBar(search_top_bar, edt_search_users)

        }
        back_search.setOnClickListener {
            hideSearchBar(search_top_bar, edt_search_users)
        }

        more_users.setOnClickListener {
            setAllUsers(false)
        }

    }





    @SuppressLint("NotifyDataSetChanged")
    private fun checking(home: HashMap<String, userInfo?>, s2: ArrayList<String>){
        updateGroupTime()
        userlist.clear()
//        Log.d("axha1", s1.toString())

        val ss= s2.size
        for (i in 1 until ss+1){
            Log.d("axha1", i.toString())
            home[s2[ss-i]]?.let { it1 -> userlist.add(it1) }
        }

        Handler().postDelayed({
            adapter.notifyDataSetChanged()
            if (dialog.isShowing){
                dialog.dismiss()
            }
            else{
                loading.isVisible = false
                recyclerView.isVisible  = true
            }
        }, 100)

    }
    private fun updateGroupTime(){
//        Log.d("axha",groupslist1.toString())
        groupslist.forEach {
            mDbRef.child("groups").child(it).child("time").addValueEventListener(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value!= null) mDbRef.child("chats").child(userUid).child("order").child(it).child("time").setValue(snapshot.value)
                }

                override fun onCancelled(error: DatabaseError) {
                    return
                }

            })
        }
    }


//    private fun setChats(){
//        mDbRef.child("chats").child(userUid).addValueEventListener(object :ValueEventListener{
//            override fun onDataChange(snapshot: DataSnapshot) {
//                myChats.clear()
//                for (snapShots in snapshot.children){
//                    val key:String = snapShots.key.toString()
//                    myChats.add(key)
////                    getChats(key)
////                    getTyping(key)
//                }
//            }
//            override fun onCancelled(error: DatabaseError) {
//                Toast.makeText(this@MainActivity, error.toString(), Toast.LENGTH_SHORT).show()
//            }
//        })
//    }
    private fun getChats(id: String){
        mDbRef.child("chats").child(userUid).child(id).child("messages").addValueEventListener(object :ValueEventListener{
            @SuppressLint("NotifyDataSetChanged", "SimpleDateFormat")
            override fun onDataChange(snapshot: DataSnapshot) {
                val msg : ArrayList<messageInfo> = ArrayList()
                var count = 0
                for (postSnap in snapshot.children){
                    if (postSnap.exists()){
                        val message = postSnap.getValue(messageInfo::class.java)
                        if (message != null) msg.add(message)
                        if (message?.senderID != userUid && message?.read!="yes"){
                            count++
                        }
                    }
                }
                unread[id] = count
                if(msg.size!=0){
                    val l = msg[msg.size-1]
                    if (count == 0) l.read = "yes"
                    val sdf = SimpleDateFormat("dd/MM/yyyy")
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DATE, -1)
                    val yesterdayL: String = sdf.format(cal.time)
                    val today: List<String> = date().split("_")
                    val date = l.time.toString()
                    val array: List<String> = date.split("_")
                    if (array.size > 1) {
                        val time = if (array[1] == today[1]) array[0] else if (array[1] == yesterdayL) "Yesterday" else array[1]
                        l.time = time
                        lastMsg[id]= l
                    }

                }
                else lastMsg[id] = null
                adapter.notifyDataSetChanged()

            }

            override fun onCancelled(error: DatabaseError) {
                return
            }

        })
    }
    private fun getGroupLastMesg (id: String){
        mDbRef.child("groups").child(id).child("chats").child("messages").addValueEventListener(object :ValueEventListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                val msg : ArrayList<messageInfo> = ArrayList()
                var count = 0
                for (postSnap in snapshot.children)
                {
                    val allow = postSnap.child("notAllow").child(userUid).exists()
                    if (postSnap.exists() && !allow){
                        val readBy = postSnap.child("readBy").child(userUid)
                        if (readBy.exists() && readBy.value == "n") {
                            count++
                        }
                        val message = postSnap.getValue(messageInfo::class.java)
                        if (message != null) msg.add(message)
                    }
                }
                unread[id] = count
                if(msg.size!=0){
                    val l = msg[msg.size-1]
                    mDbRef.child("user").child(l.senderID.toString()).child("name").addListenerForSingleValueEvent(object: ValueEventListener{
                        @SuppressLint("SimpleDateFormat")
                        override fun onDataChange(snapshot: DataSnapshot) {
                           if (snapshot.exists()) {
                               groupPersonName[id] = snapshot.value.toString()
                               if (count == 0) l.read = "yes"
                               val sdf = SimpleDateFormat("dd/MM/yyyy")
                               val cal = Calendar.getInstance()
                               cal.add(Calendar.DATE, -1)
                               val yesterdayL: String = sdf.format(cal.time)
                               val today: List<String> = date().split("_")
                               val date = l.time.toString()
                               val array: List<String> = date.split("_")
                               if (array.size > 1) {
                                   val time = if (array[1] == today[1]) array[0] else if (array[1] == yesterdayL) "Yesterday" else array[1]
                                   l.time = time
                                   lastMsg[id]=l
                               }

                           }
                            adapter.notifyDataSetChanged()
                        }
                        override fun onCancelled(error: DatabaseError) {
                            return
                        }
                    })

                }
                else {
                    lastMsg[id] = null
                    adapter.notifyDataSetChanged()

                }
            }
            override fun onCancelled(error: DatabaseError) {
                return
            }

        })
    }
    private fun getGroupTyping(id: String){
        mDbRef.child("groups").child(id).child("chats").child("status").addValueEventListener(object: ValueEventListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists() && snapshot.value!="") {
                    val groupStatusFull =  snapshot.value.toString().trim().split("\\s+".toRegex())
                    typing[id] = "${groupStatusFull[1]} is typing"
                }
                else{
                    typing[id] = ""
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                return
            }
        })
    }

    private fun getTyping(id: String){
        mDbRef.child("chats").child(id).child(userUid).child("status").addValueEventListener(object : ValueEventListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.value!="") {
                    typing[id] = "typing..."
                }
                else typing[id] = ""
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                return
            }

        })
    }

    private fun filterSearch(text: String, fAdapter: userAdapter, list: ArrayList<userInfo>){
        val filtered:ArrayList<userInfo> = ArrayList()
        list.forEach {
            Log.d("hkhk", it.toString())
            if ( it.userID == null ){
                if (it.name!!.lowercase().contains(text.lowercase())) filtered.add(it)
            }
            else if (it.name!!.lowercase().contains(text.lowercase()) || it.userID!!.lowercase().contains(text.lowercase())){
                filtered.add(it)
            }
        }
        fAdapter.filerList(filtered)
    }
    private fun showSearchBar(topBar: LinearLayout, edtSearch: EditText){
        edtSearch.requestFocus()
        showSoftKeyboard(edtSearch)
        topBar.isVisible = true
    }
    private fun hideSearchBar(topBar: LinearLayout, edtSearch: EditText){
        edtSearch.setText("")
        hideSoftKeyboard(edtSearch)
        topBar.isVisible = false
    }
    private fun showSoftKeyboard(view: View) {
        if (view.requestFocus()) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    private fun hideSoftKeyboard(view: View) {
        if (view.requestFocus()) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow( view.windowToken, 0)
        }
    }



    private fun allUsersLayout(view: View, alluserlist: ArrayList<userInfo>, group: Boolean){
        dialogAllUser.show()
        val searchBarAllUser: LinearLayout = viewAllUser.findViewById(R.id.search_allUser_bar)
        val edtSearchAllUser: EditText = viewAllUser.findViewById(R.id.edt_search_allUser)
        val bk: ImageView = viewAllUser.findViewById(R.id.all_users_back_button)


        viewAllUser.findViewById<RelativeLayout>(R.id.create_Group).isVisible = group
        val adapter_all_users = userAdapter(null, this, alluserlist, null, null, null, dialogAllUser, group, null, null)
        viewAllUser.all_users_recyclerView.layoutManager = LinearLayoutManager(this)
        viewAllUser.all_users_recyclerView.adapter = adapter_all_users


        bk.all_users_back_button.setOnClickListener {
            dialogAllUser.dismiss()
        }
        viewAllUser.search_all_user.setOnClickListener {
            showSearchBar(searchBarAllUser, edtSearchAllUser)
        }
        viewAllUser.back_search_allUser.setOnClickListener {
            hideSearchBar(searchBarAllUser, edtSearchAllUser)
        }
        edtSearchAllUser.addTextChangedListener {
            val txt: String = edtSearchAllUser.text.toString()
            if (txt != "") filterSearch(txt, adapter_all_users, alluserlist) else adapter_all_users.filerList(alluserlist)
        }
    }

    private fun setAllUsers(group: Boolean){
        mDbRef.child("user").addListenerForSingleValueEvent(object :ValueEventListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                alluserlist.clear()
//                Log.d("moreCHats","k ${myChats.toString()}" )
                for (postSnapShot in snapshot.children){
                    val currentUser = postSnapShot.getValue(userInfo::class.java)
                    if (currentUser?.uid != userUid ){
                        if (!group && currentUser?.uid !in myChats){
                            alluserlist.add(currentUser!!)
                            Log.d("axha1", "false")
                        }
                        else if(group){
                            alluserlist.add(currentUser!!)
                            Log.d("axha1", "true")
                        }
                    }
                }
                if (group) allUsersLayout(viewAllUser, alluserlist ,group)
                else allUsersLayout(viewAllUser, alluserlist ,group)

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, error.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun checkActivity(dialog: Dialog) {
        if (checkTime())(
            if(userUid == "null"){
                val intent = Intent(this@MainActivity, login::class.java)
                dialog.dismiss()
                finish()
                startActivity(intent)
            }
        )
        else{
            dialog()
        }
    }
    private fun dialog(){
        AlertDialog.Builder(this)
            .setMessage("This app only available in Pakistan. If you in Pakistan then check your time setting")
            .setPositiveButton("OK", DialogInterface.OnClickListener { dialogInterface, i ->
                finish()
            })
            .show()
    }
    @SuppressLint("SimpleDateFormat")
    private fun checkTime(): Boolean{
        val s= Date()
        val sdf1 = SimpleDateFormat("h:mm a")
        val timezoneS = "Asia/Karachi"
        val tz = TimeZone.getTimeZone(timezoneS)
        TimeZone.setDefault(tz)
        val sdf2 = SimpleDateFormat("h:mm a")

        if(sdf1.format(s).equals(sdf2.format(s))){
            return true
        }
        return false

    }

    private fun deleteAccount(){
        if (userUid!=null) {
            mAuth.currentUser?.delete()
                ?.addOnCompleteListener {
                    Toast.makeText(this@MainActivity, "delete", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, login::class.java)
                    dialog.dismiss()
                    finish()
                    startActivity(intent)
                }
        }
    }


    private fun showPopup(view: View) {
        val popup = PopupMenu(this, view)
        popup.inflate(R.menu.menu_main_activity)

        popup.setOnMenuItemClickListener { item: MenuItem? ->
            when (item!!.itemId) {
                R.id.profile -> {
                    val intent = Intent(this, user_profile::class.java)
                    intent.putExtra("myProfile", "True")
                    startActivity(intent)
                }
                R.id.setting -> {
                    Toast.makeText(this@MainActivity, "Setting", Toast.LENGTH_SHORT).show()
                }
                R.id.logout -> {
                    mAuth.signOut()
                    val intent = Intent(this, login::class.java)
                    finish()
                    startActivity(intent)
                }
                R.id.group -> {
                    setAllUsers(true)
                }
            }

            true
        }

        popup.show()
    }


    override fun onDestroy() {
        mDbRef.child("user").child(userUid).child("status").setValue(date())
        super.onDestroy()
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        edt_search_users.setText("")
        search_top_bar.isVisible=false
        super.onResume()
    }

    override fun onStop() {

        edt_search_users.setText("")
        search_top_bar.isVisible=false
        super.onStop()
    }

    @SuppressLint("SimpleDateFormat")
    private fun date():String{
        val sdf = SimpleDateFormat("h:mm a_dd/MM/yyyy")

        return sdf.format(Date())
    }
    @SuppressLint("SimpleDateFormat")
    private fun date1(s: Long?):String{
//        val sdf = SimpleDateFormat("h:mm a_dd/MM/yyyy")
        val sdf = SimpleDateFormat("h:mm a_dd/MM/yyyy", Locale.ENGLISH)

        if (s != null) {
            return sdf.format(s)
        }
        else return ""
    }



    @TargetApi(Build.VERSION_CODES.M)
    fun isNetworkOnline1(context: Context): Boolean {
        var isOnline = false
        try {
            val manager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities =
                manager.getNetworkCapabilities(manager.activeNetwork) // need ACCESS_NETWORK_STATE permission
            isOnline =
                capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return isOnline
    }

    override fun off() {
        mainActivityBar.isVisible = false
    }

    override fun on() {
        mainActivityBar.isVisible = true
    }

}