package com.example.chatnow

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.android.synthetic.main.activity_splash_screen.*


class splash_screen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var splashTime: Long = 5000




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        supportActionBar?.hide()
        auth = FirebaseAuth.getInstance()

        var dialog: Dialog? = null

        val builder = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen)
        val view = layoutInflater.inflate(R.layout.fragment_splash, null)
        builder.setView(view)
        dialog = builder.create()
        dialog.show()

        if(checkTime()){
            if(auth.currentUser == null){
                intent = Intent(this@splash_screen, login::class.java)
            }
            else{
                intent =Intent(this@splash_screen, MainActivity::class.java)
            }

            Handler().postDelayed({
                startActivity(intent)
                finish()
            }, splashTime)
        }
        else{
            progress.isVisible = false
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
}