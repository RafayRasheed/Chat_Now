package com.example.chatnow

import android.media.Image

class userInfo {
    var uid: String? = null
    var name: String? = null
    var userID: String? = null
    var email: String? = null
    var image: String? = null
    var status: String? = null



    constructor()
    constructor(uid: String?, name: String?, userID: String?, email:String?, image: String?, status:String?){
        this.uid = uid
        this.name = name
        this.userID = userID
        this.email = email
        this.image = image
        this.status = status
    }
}