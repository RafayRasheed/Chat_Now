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
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*


class login : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    val backRed: Int = R.drawable.edt_red_background
    val backNor: Int = R.drawable.edt_backgroung
    var  i: Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        var dialog: AlertDialog?
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.fragment_loading, null)
        builder.setView(view)
        dialog = builder.create()
        dialog.window?.attributes?.windowAnimations =  R.style.DialogTheme
        val window: Window? = dialog!!.window
        window?.setGravity(Gravity.CENTER)

//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Animation Dialog")
//        builder.setMessage("type")
//        builder.setNegativeButton("OK", null)
//        val dialog = builder.create()
//        dialog.window?.attributes?.windowAnimations =  R.style.DialogTheme


        mAuth= FirebaseAuth.getInstance()


        show_password.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    edt_paswword.transformationMethod = HideReturnsTransformationMethod.getInstance();
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    edt_paswword.transformationMethod = PasswordTransformationMethod.getInstance();
                    edt_paswword.setSelection(edt_paswword.text.length)
                }
            }
            v?.onTouchEvent(event) ?: true
        }

        edt_email.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> edt_email.setBackgroundResource(backNor)
            }
            v?.onTouchEvent(event) ?: true
        }

        edt_paswword.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> edt_paswword_layout.setBackgroundResource(backNor)
            }

            v?.onTouchEvent(event) ?: true
        }

        btn_signUp.setOnClickListener{
            val intent = Intent(this@login, signUp::class.java)
            startActivity(intent)
        }

        btn_login.setOnClickListener{
            val email = edt_email.text.toString()
            val password = edt_paswword.text.toString()
            if(checkForInternet(this@login)){
                if (isNetworkOnline1(this@login)){
                    if(signIn_check(email, password)) {
                        dialog.show()
                        signIn(email, password, dialog)
                    }
                }
                else{
                    dialog.dismiss()
                    Toast.makeText(this@login,"No Internet Access",Toast.LENGTH_SHORT).show()
                }
            }
            else{
                dialog.dismiss()
                Toast.makeText(this@login,"Internet Not Connected",Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun signIn(email: String, password: String, dialog:Dialog){

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this@login) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this@login, MainActivity::class.java)
                    intent.putExtra("fromLogin",true)
                    dialog.dismiss()
                    finish()
                    startActivity(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    dialog.dismiss()
                    Toast.makeText(baseContext, "Incorrect Email or Password",
                        Toast.LENGTH_SHORT).show()
                }
            }

    }

    // Check Internet Access
//    private fun internetIsConnected(): Boolean {
//        return try {
//            val command = "ping -c 1 google.com"
//            Runtime.getRuntime().exec(command).waitFor() == 0
//        } catch (e: Exception) {
//            false
//        }
//    }

    private fun signIn_check(email: String, password: String): Boolean{
        i = true
        if(email==""){
            edt_email.setBackgroundResource(backRed)
            i = false
        }
        if (password=="") {
            edt_paswword_layout.setBackgroundResource(backRed)
            i = false
        }
        return i
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
            val capabilities =
                manager.getNetworkCapabilities(manager.activeNetwork) // need ACCESS_NETWORK_STATE permission
            isOnline =
                capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return isOnline
    }
}




