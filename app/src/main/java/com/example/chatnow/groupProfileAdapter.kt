package com.example.chatnow

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
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
import kotlin.collections.ArrayList

class groupProfileAdapter (val context: Context, var userList: ArrayList<userInfo>, val dialog: Dialog, val adminList: ArrayList<String>, val groupUid: String):
    RecyclerView.Adapter<groupProfileAdapter.userViewHolder>() {
    lateinit var viewGroup: ViewGroup
    lateinit var view: View
    var selected: java.util.ArrayList<userInfo> = java.util.ArrayList()
    val userUid = FirebaseAuth.getInstance().currentUser?.uid.toString()


    val mDbRef: DatabaseReference = FirebaseDatabase.getInstance().reference


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): userViewHolder {
        viewGroup = parent
        view = LayoutInflater.from(context).inflate(R.layout.userlist_layout, parent , false)

        return userViewHolder(view)
    }
    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: userViewHolder, position: Int) {
        Log.d("okok", "ok")
        val currentUser = userList[position]



        if(currentUser.image != ""){
            Glide.with(context)
                .asBitmap()
                .load(currentUser.image)
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
        holder.text_userID.text= currentUser.userID
        holder.statusOnline.isVisible = false
        holder.unread.isVisible = false

        holder.admin.isVisible = !dialog.isShowing && currentUser.uid in adminList

        if (dialog.isShowing && currentUser in selected){
            holder.all_user.setBackgroundColor(Color.LTGRAY)
        }
        else{
            holder.all_user.setBackgroundColor(Color.parseColor("#FAFAFA"))
        }


        holder.itemView.setOnLongClickListener {
            val uid = currentUser.uid
            if (!dialog.isShowing && userUid in adminList && uid != userUid){
                val dialog: Dialog?
                val builder = AlertDialog.Builder(context)
                val view = LayoutInflater.from(context).inflate(R.layout.fragment_edt_profile_options, null)

                val fragProfBtnChange: Button = view.findViewById(R.id.FragProfBtnChange)
                val fragProfBtnRemove: Button = view.findViewById(R.id.FragProfBtnRemove)
                val fragProfBtnCancel: Button = view.findViewById(R.id.FragProfBtnCancel)

                builder.setView(view)
                dialog = builder.create()
                val admin = uid in adminList
                fragProfBtnChange.text = if (admin) "Remove From Admin" else "Make An Admin"
                fragProfBtnRemove.text = "Remove From Group"
                dialog.show()

                fragProfBtnChange.setOnClickListener {
                    val s = if (admin) "n" else "y"
                    mDbRef.child("groups").child(groupUid).child("members").child(uid.toString()).setValue(s)
                    dialog.cancel()
                }

                fragProfBtnRemove.setOnClickListener {
                    mDbRef.child("groups").child(groupUid).child("members").child(uid.toString()).setValue(null)
                    mDbRef.child("chats").child(uid.toString()).child(groupUid).setValue(null)
                    dialog.cancel()

                }
                fragProfBtnCancel.setOnClickListener {
                    dialog.cancel()
                }

            }

            return@setOnLongClickListener true
        }

        holder.itemView.setOnClickListener {
            if (dialog.isShowing) {
                val shareBut = dialog.findViewById<ImageView>(R.id.share_msg_button)
                if (currentUser in selected) {
                    selected.remove(currentUser)
                    notifyItemChanged(position)
                    if (selected.isEmpty()){
                        shareBut.isVisible = false
                    }
                }

                else{
                    selected.add(currentUser)
                    notifyItemChanged(position)
                    if (!shareBut.isVisible) shareBut.isVisible = true
                }

                shareBut.setOnClickListener {
                    val dialog1: AlertDialog?
                    val builder = AlertDialog.Builder(context)
                    val view = LayoutInflater.from(context).inflate(R.layout.fragment_loading, null)
                    builder.setView(view)
                    dialog1 = builder.create()
                    dialog1.window?.attributes?.windowAnimations =  R.style.DialogTheme
                    val window: Window? = dialog1!!.window
                    window?.setGravity(Gravity.CENTER)
                    dialog1.show()
                    selected.forEach {
                        mDbRef.child("groups").child(groupUid).child("members").child(it.uid.toString()).setValue("n")
                        mDbRef.child("chats").child(it.uid.toString()).child("order").child(groupUid).child("group").setValue(true)
                        mDbRef.child("chats").child(it.uid.toString()).child("order").child(groupUid).child("time").setValue(date1().toLongOrNull())
                            .addOnCompleteListener{
                                dialog1.dismiss()
                                dialog.cancel()
                            }
                    }
                }


            }
            else if(currentUser.uid != userUid){
                val intent = Intent(context, chatActivity::class.java)
                intent.putExtra("personUid", currentUser.uid)
                intent.putExtra("personName", currentUser.name)
                intent.putExtra("personUserID", currentUser.userID)
                intent.putExtra("personImage", currentUser.image)
                intent.putExtra("personEmail", currentUser.email)
                context.startActivity(intent)

            }

        }
    }
    override fun getItemCount(): Int {
        return userList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filerList(filter: java.util.ArrayList<userInfo>){
        userList = filter
        notifyDataSetChanged()
    }
    @SuppressLint("SimpleDateFormat")
    private fun date1():String{
//        val sdf = SimpleDateFormat("h:mm a_dd/MM/yyyy")
        val sdf = SimpleDateFormat("yyyyMMddHHmmssSS")

        return sdf.format(Date())
    }


    class userViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val text_name = itemView.findViewById<TextView>(R.id.txt_name)
        val text_userID = itemView.findViewById<TextView>(R.id.txt_userID)
        val statusOnline = itemView.findViewById<ImageView>(R.id.online)
        val unread = itemView.findViewById<TextView>(R.id.unreadMsg)
        val admin = itemView.findViewById<TextView>(R.id.adminTag)
        val all_user = itemView.findViewById<RelativeLayout>(R.id.user_layout_full)

    }




}
