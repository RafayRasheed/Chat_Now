package com.example.chatnow

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_user_profile.*
import java.text.SimpleDateFormat
import java.util.*


class user_profile : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    private lateinit var userUid: String
    private lateinit var image: String
    private lateinit var name: String
    private lateinit var userID: String
    private lateinit var email: String
    private  var imageBitmap: Bitmap? = null

    @SuppressLint("ShowToast", "CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference("user")
        userUid = mAuth.currentUser?.uid.toString()


        setContentView(R.layout.activity_user_profile)

        val myProfile = intent.getStringExtra("myProfile")

//        val database = Firebase.database
//        val myConnectionsRef = database.getReference("user").child(userUid).child("status")
//
//// Stores the timestamp of my last disconnect (the last time I was seen online)
//        val lastOnlineRef = database.getReference("/users/joe/lastOnline")
//
//        val connectedRef = database.getReference(".info/connected")
//        connectedRef.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val connected = snapshot.getValue<Boolean>() ?: false
//                if (connected) {
//                    val con = myConnectionsRef.push()
//
//                    // When this device disconnects, remove it
//                    con.onDisconnect().removeValue()
//
//                    // When I disconnect, update the last time I was seen online
//                    lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP)
//
//                    // Add this device to my connections list
//                    // this value could contain info about the device or a timestamp too
//                    con.setValue(java.lang.Boolean.TRUE)
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//            }
//        })

        userProfileBackButton.setOnClickListener {
            onBackPressed()

//            if (myProfile == "False"){
//                onBackPressed()
//                return@setOnClickListener
//            }
//            else{
//                val intent = Intent(this, MainActivity::class.java)
//                startActivity(intent)
//                finish()
//            }


        }
        if (myProfile == "False") {
            edtTxt_profileImage.visibility = View.INVISIBLE
            edtTxt_name.visibility = View.INVISIBLE
            edtTxt_username.visibility = View.INVISIBLE



            val personImage = intent.getStringExtra("personImage").toString()
            nameTxt.text = intent.getStringExtra("personName").toString()
            usernameTxt.text = intent.getStringExtra("personUserID").toString()
            emailTxt.text = intent.getStringExtra("personEmail").toString()

            if (personImage != "") {
                loadingImage(personImage)
                userProfileImage.setOnClickListener{
                    FragShowImage()
                }
            }
            else{
//                userProfileImage.setImageDrawable(null)
                offLoading()
            }


        }
        else {

            firebaseDataChange("")

            userProfileImage.setOnClickListener {
                if (image != "" && imageBitmap!=null) {
                    FragShowImage()
                }
            }

            edtTxt_profileImage.setOnClickListener {
                if (image != "") {
                    FragEdtProfile()
                } else {
                    selelecImage()
                }
            }

            edtTxt_name.setOnClickListener {
                FragNameUsername("Name", name)
            }

            edtTxt_username.setOnClickListener {
                FragNameUsername("Username", userID)
            }
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
            firebaseDataChange("")
            dialog.cancel()

        }
        fragProfBtnCancel.setOnClickListener {
            dialog.cancel()
        }




    }


    private fun selelecImage(){
        val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
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

    private fun FragNameUsername(type: String, previous: String) {
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
        window?.setGravity(Gravity.BOTTOM)
        dialog.show()



        fragHeading.text = type
        fragmentEdt.setText(previous)


        fragCancel.setOnClickListener {
            dialog.cancel()
        }

        fragSave.setOnClickListener {
            val text: String = fragmentEdt.text.toString()
            if (text != ""){
                if (type == "Name"){
                    mDbRef.child(userUid).child("name").setValue(text)
                }
                else{
                    mDbRef.child(userUid).child("userID").setValue(text)
                }
                firebaseDataChange("name")
                dialog.cancel()
            }
        }

    }


    private fun firebaseDataChange(type: String) {
        mDbRef.child(userUid).addValueEventListener(object :ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                val currenUser = snapshot.getValue(userInfo::class.java)
                name = currenUser?.name.toString()
                userID = currenUser?.userID.toString()
                image = currenUser?.image.toString()
                email = currenUser?.email.toString()

                nameTxt.text= name
                usernameTxt.text = userID
                emailTxt.text = email

                if (type == ""){
                    if (image != "") {

                        loadingImage(image)
                    }
                    else{
                        offLoading()
                    }
                }

            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@user_profile, error.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private var selectedPhotoUri: Uri? = null

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            selectedPhotoUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)
//            userProfileImage.setImageBitmap(bitmap)
            uploadImageToFirebaseStorage()

        }
    }

    private fun uploadImageToFirebaseStorage(){
        if (selectedPhotoUri == null) return

//        val filname: String = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$userUid")
        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    imageTODatabase(it.toString())
                }
            }
//        Toast.makeText(this, i.toString(), Toast.LENGTH_SHORT).show()

    }
    private fun imageTODatabase(imageUrL: String){
        mDbRef.child(userUid).child("image").setValue(imageUrL)
    }

    private fun getBitmapImage(imageURL: String ){
        if (!this.isFinishing){
            Glide.with(this@user_profile)
                .asBitmap()
                .load(imageURL)
                .into(object : CustomTarget<Bitmap>(){
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        userProfileImage.setImageBitmap(resource)
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
            loading1.isVisible = false
        }, 200)

    }
    private fun offLoading(){
        loading1.isVisible = false
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
        mDbRef.child(userUid).child("status").setValue(status)
    }

    @SuppressLint("SimpleDateFormat")
    private fun date():String{
        val sdf = SimpleDateFormat("h:mm a_dd/MM/yyyy")
        return sdf.format(Date())
    }


}


