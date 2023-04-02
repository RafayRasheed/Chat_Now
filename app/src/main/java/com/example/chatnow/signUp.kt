package com.example.chatnow

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.android.synthetic.main.activity_sign_up.*
import java.text.SimpleDateFormat
import java.util.*

class signUp : AppCompatActivity() {
    val backRed: Int = R.drawable.edt_red_background
    val backNor: Int = R.drawable.edt_backgroung
    var  i:Boolean = true
    private lateinit var mAuth:FirebaseAuth
    private lateinit var mDbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        mAuth=FirebaseAuth.getInstance()

        var dialog: AlertDialog?
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.fragment_loading, null)
        builder.setView(view)
        dialog = builder.create()
        val window: Window? = dialog!!.window
        window?.setGravity(Gravity.CENTER)

        signUpBackButton.setOnClickListener {
            onBackPressed()
        }

        edt_name.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> edt_name.setBackgroundResource(backNor)
                }

                return v?.onTouchEvent(event) ?: true
            }
        })
        edt_userID.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> edt_userID.setBackgroundResource(backNor)
                }

                return v?.onTouchEvent(event) ?: true
            }
        })

        edt_email.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> edt_email.setBackgroundResource(backNor)
                }
                return v?.onTouchEvent(event) ?: true
            }
        })

        edt_paswword.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> edt_paswword.setBackgroundResource(backNor)
                }

                return v?.onTouchEvent(event) ?: true
            }
        })

        btn_signUp.setOnClickListener{
            val name = edt_name.text.toString()
            val email = edt_email.text.toString()
            val password = edt_paswword.text.toString()
            val userID = edt_userID.text.toString()
            signUp_check(name, email, password, userID)

            if(checkForInternet(this)){
                if (isNetworkOnline1(this)){
                    if (i) {
                        dialog.show()
                        signUp(name, userID, email, password, dialog)
                    }
                }
                else{
                    dialog.dismiss()
                    Toast.makeText(this,"No Internet Access",Toast.LENGTH_SHORT).show()
                }
            }
            else{
                dialog.dismiss()
                Toast.makeText(this,"Internet Not Connected",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signUp(name: String, userID: String, email: String, password: String, dialog:Dialog){
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in Success, update UI with the signed-in user's information
                    userSaveDatabase(name, userID, email, mAuth.currentUser?.uid!!, date())
                    Toast.makeText(baseContext, "Account Created Successfully",
                        Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, login::class.java)
                    dialog.dismiss()
                    finish()
                    startActivity(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    dialog.dismiss()
                    Toast.makeText(baseContext, "Failed to create an account.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun signUp_check(name: String, userID: String, email: String, password: String){
        i = true
        if(name==""){
            edt_name.setBackgroundResource(backRed)
            i = false
        }
        if (email=="") {
            edt_email.setBackgroundResource(backRed)
            i = false
        }
        if (password==""){
            edt_paswword.setBackgroundResource(backRed)
            i = false
        }
        if (userID==""){
            edt_userID.setBackgroundResource(backRed)
            i = false
        }

    }

    @SuppressLint("SimpleDateFormat")
    private fun date():String{
        val sdf = SimpleDateFormat("h:mm a_dd/MM/yyyy")
        return sdf.format(Date())
    }


    private fun userSaveDatabase(name: String, userID: String, email: String, uid:String, date: String){
        mDbRef = FirebaseDatabase.getInstance().getReference()
        mDbRef.child("user").child(uid).setValue(userInfo(uid, name, userID, email, "", ""))
        mDbRef.child("user").child(uid).child("lastOnline").setValue(ServerValue.TIMESTAMP)
    }

    private fun checkForInternet(context: Context): Boolean {

        // register activity with the connectivity manager service
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            // if the android version is below M
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun isNetworkOnline1(context: Context): Boolean {
        var isOnline = false
        try {
            val manager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) // need ACCESS_NETWORK_STATE permission
            isOnline = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return isOnline
    }

}
