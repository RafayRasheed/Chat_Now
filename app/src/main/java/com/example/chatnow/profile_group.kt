package com.example.chatnow

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_profile_group.*
import kotlinx.android.synthetic.main.all_users_list.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class profile_group : AppCompatActivity() {
    lateinit var adapter: groupProfileAdapter
    private lateinit var mdbRef: DatabaseReference
    private  var memberlist: ArrayList<userInfo> = ArrayList()
    private  var allmemebers: ArrayList<String> = ArrayList()
    private  var adminlist: ArrayList<String> = ArrayList()

    private lateinit var groupInfo: userInfo
    private lateinit var viewAllUser: View
    private lateinit var dialogAllUser: Dialog
    private lateinit var userUid: String
    private lateinit var mAuth: FirebaseAuth
    private lateinit var groupUid: String
    private  var image = ""
    private var imageBitmap: Bitmap? = null

    private var youAdmin = false





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_group)

        mAuth = FirebaseAuth.getInstance()
        userUid = mAuth.currentUser?.uid.toString()
        mdbRef = FirebaseDatabase.getInstance().reference
        groupUid = intent.getStringExtra("groupUid").toString()

        val builderAllUser = AlertDialog.Builder(this, android.R.style.ThemeOverlay_Material_Light)
        viewAllUser = layoutInflater.inflate(R.layout.all_users_list, null)
        builderAllUser.setView(viewAllUser)
        dialogAllUser = builderAllUser.create()
        dialogAllUser.window?.attributes?.windowAnimations =  R.style.DialogTheme

        adapter = groupProfileAdapter(this, memberlist, dialogAllUser, adminlist, groupUid)
        val userRecyclerView: RecyclerView =  findViewById(R.id.all_users_recyclerView)
        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userRecyclerView.adapter = adapter
        userRecyclerView.layoutManager = LinearLayoutManager(this)


        mdbRef.child("groups").child(groupUid).child("info").addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    groupInfo = snapshot.getValue(userInfo::class.java)!!
                    nameTxtGroup.text = groupInfo.name
                    if (image != groupInfo.image) {
                        image = groupInfo.image.toString()
                        if (image!="") loadingImage(image) else  offLoading()
                    }
                    else{
                        offLoading()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                return
            }
        })
        setMembers()

        edtTxt_name_Group.setOnClickListener {
            FragNameUsername(nameTxtGroup.text.toString())
        }
        userProfileImageGroup.setOnClickListener {
            if (image != "" && imageBitmap != null) {
                FragShowImage()
            }
        }
        userProfileBackButtonGroup.setOnClickListener {
            onBackPressed()
        }
        edtTxt_profileImageGroup.setOnClickListener {
            if (image != "") {
                FragEdtProfile()
            } else {
                selelecImage()
            }
        }
        addImage_group_members.setOnClickListener {
            addmembers()
        }

    }
    private fun setMembers(){
        mdbRef.child("groups").child(groupUid).child("members").addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val members: ArrayList<String> = ArrayList()
                adminlist.clear()
                allmemebers.clear()
                for(postSnap in snapshot.children){
                    val v = postSnap.value.toString()
                    val k = postSnap.key.toString()
                    if (k == userUid){
                        youAdmin = postSnap.value == "y"
                        addGroupMembers.isVisible = youAdmin
                        edtTxt_profileImageGroup.isVisible = youAdmin
                        edtTxt_name_Group.isVisible = youAdmin
                    }
                    else if(v == "y") adminlist.add(k) else members.add(k)
                    allmemebers.add(k)
                }
                Log.d("hahaha", "$adminlist  ||  $members  || $allmemebers")
                var last = userUid
                if (members.size != 0) last = members[members.size-1] else if (adminlist.size != 0) last = adminlist[adminlist.size-1]
                memberlist.clear()
                getPersonInfo(userUid , last)
                adminlist.forEach {
                    getPersonInfo(it, last)
                }
                members.forEach {
                    getPersonInfo(it, last)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                return
            }
        })
    }
    private fun getPersonInfo(uid: String, last:String){
        mdbRef.child("user").child(uid).addListenerForSingleValueEvent(object :ValueEventListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    val user = snapshot.getValue(userInfo::class.java)
                    if (user != null) {
                        if (user.uid == userUid) user.name = "You"
                        memberlist.add(user)
                    }

                }
                if (uid == last){
                    if (youAdmin) adminlist.add(userUid)

                    adapter.notifyDataSetChanged()}
            }

            override fun onCancelled(error: DatabaseError) {
                return
            }
        })
    }

    private fun addmembers(){
        mdbRef.child("user").addListenerForSingleValueEvent(object :ValueEventListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                val userlist: ArrayList<userInfo> = ArrayList()
                for (shotSnap in snapshot.children){
                    val user = shotSnap.getValue(userInfo::class.java)
                    if (user != null) {
                        if (user.uid !in allmemebers) userlist.add(user)
                    }
                }
                allUsersLayout(userlist)
            }

            override fun onCancelled(error: DatabaseError) {
                return
            }
        })
    }

    private fun allUsersLayout(user: ArrayList<userInfo>){

        viewAllUser.findViewById<RelativeLayout>(R.id.create_Group).isVisible = false
        val adapter_all_users = groupProfileAdapter(this, user, dialogAllUser, adminlist, groupUid)
        viewAllUser.all_users_recyclerView.layoutManager = LinearLayoutManager(this)
        viewAllUser.all_users_recyclerView.adapter = adapter_all_users

        dialogAllUser.show()

        val searchBarAllUser: LinearLayout = viewAllUser.findViewById(R.id.search_allUser_bar)
        val edtSearchAllUser: EditText = viewAllUser.findViewById(R.id.edt_search_allUser)
        val bk: ImageView = viewAllUser.findViewById(R.id.all_users_back_button)

        searchBarAllUser.isVisible = true
        bk.all_users_back_button.setOnClickListener {
            dialogAllUser.dismiss()
        }
        edtSearchAllUser.addTextChangedListener {
            val txt: String = edtSearchAllUser.text.toString()
            if (txt != "") filterSearch(txt, adapter_all_users, user) else adapter_all_users.filerList(user)
        }
        dialogAllUser.setOnDismissListener {
            setMembers()
        }

    }

    private fun filterSearch(text: String, fAdapter: groupProfileAdapter, list: ArrayList<userInfo>){
        val filtered:ArrayList<userInfo> = ArrayList()
        list.forEach {
            if (it.name!!.lowercase().contains(text.lowercase()) || it.userID!!.lowercase().contains(text.lowercase())){
                filtered.add(it)
            }
        }
        fAdapter.filerList(filtered)
    }

    @SuppressLint("SetTextI18n")
    private fun FragNameUsername(previous: String) {
        val dialog: AlertDialog?
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.fragment_frag_edt_name_user_name, null)

        val fragHeading: TextView = view.findViewById(R.id.FragmentHeading)
        val fragmentEdt: EditText = view.findViewById(R.id.FragmentEdt)

        val fragCancel: TextView = view.findViewById(R.id.FragmentCancel)
        val fragSave: TextView = view.findViewById(R.id.FragmentSave)

        builder.setView(view)
        dialog = builder.create()

        val window: Window? = dialog!!.window
        window?.setGravity(Gravity.CENTER)
        dialog.show()

        fragHeading.text = "Group Name"
        fragmentEdt.setText(previous)

        fragCancel.setOnClickListener {
            dialog.cancel()
        }

        fragSave.setOnClickListener {
            val text: String = fragmentEdt.text.toString()
            if (text != ""){
                mdbRef.child("groups").child(groupUid).child("info").child("name").setValue(text)
                dialog.cancel()
            }
        }
    }



    private fun FragShowImage(){

        var dialog: Dialog? = null
        val builder = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val view = layoutInflater.inflate(R.layout.fragmnet_image_show, null)

        val fragProfileImage: ImageView = view.findViewById(R.id.FragProfileImage)
        val fragProfileCancel: ImageView = view.findViewById(R.id.FragProfileCancel)

        builder.setView(view)
        dialog = builder.create()
        dialog.show()
        fragProfileImage.setImageBitmap(imageBitmap)

        fragProfileCancel.setOnClickListener {
            dialog.cancel()
        }
    }


    private fun FragEdtProfile(){
        var dialog: Dialog? = null
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.fragment_edt_profile_options, null)

        val fragProfBtnChange: Button = view.findViewById(R.id.FragProfBtnChange)
        val fragProfBtnRemove: Button = view.findViewById(R.id.FragProfBtnRemove)
        val fragProfBtnCancel: Button = view.findViewById(R.id.FragProfBtnCancel)

        builder.setView(view)
        dialog = builder.create()
        dialog.show()

        fragProfBtnChange.setOnClickListener {
            selelecImage()
            dialog.cancel()
        }

        fragProfBtnRemove.setOnClickListener {
            imageTODatabase("")
            dialog.cancel()

        }
        fragProfBtnCancel.setOnClickListener {
            dialog.cancel()
        }

    }


    private fun imageTODatabase(value : String) {
        mdbRef.child("groups").child(groupUid).child("info").child("image").setValue(value)
            .addOnSuccessListener {
                if (value == "") userProfileImageGroup.setImageResource(R.drawable.profile)
            }
    }


    private fun selelecImage(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 0)
    }


    private fun getBitmapImage(imageURL: String ){
        if (!this.isFinishing){
            Glide.with(this@profile_group)
                .asBitmap()
                .load(imageURL)
                .into(object : CustomTarget<Bitmap>(){
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        userProfileImageGroup.setImageBitmap(resource)
                        imageBitmap = resource
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
        }
    }


    private fun loadingImage(imageURL: String){
        Handler().postDelayed({
            getBitmapImage(imageURL)
            loading1Group.isVisible = false
        }, 200)
    }


    private fun offLoading(){
        loading1Group.isVisible = false
    }


    override fun onPause() {
        setStatus(date())
        super.onPause()
    }


    override fun onStart() {
        setStatus("online")
        super.onStart()
    }


    private fun setStatus(status: String){
        mdbRef.child("user").child(userUid).child("status").setValue(status)
    }


    @SuppressLint("SimpleDateFormat")
    private fun date():String{
        val sdf = SimpleDateFormat("h:mm a_dd/MM/yyyy")
        return sdf.format(Date())
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            loading1Group.isVisible = true
            val selectedPhotoUri = data.data
            val ref = FirebaseStorage.getInstance().getReference("/images/$groupUid")
            ref.putFile(selectedPhotoUri!!)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener {
                        imageTODatabase(it.toString())
                    }
                }
        }
    }

}