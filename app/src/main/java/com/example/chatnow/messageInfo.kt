package com.example.chatnow

class messageInfo {
    var message:String? = null
    var senderID:String? = null
    var time: String? = null
    var read: String? = null
    var type: String? = null
    var source:String? = null
    var messageKey:String? = null
    var forward:String? = null
    var name:String? = null



    constructor()
    constructor(message: String?, type: String?, source: String?, senderID: String?, time: String?, read: String?, forward:String?, name:String? ){
        this.message = message
        this.type = type
        this.source = source
        this.senderID = senderID
        this.time = time
        this.read = read
        this.forward = forward
        this.name = name

    }
    constructor(message: String?,  type: String?, source: String?, senderID: String?, time: String?, read: String?, forward:String?, name:String?,  messageKey: String){
        this.message = message
        this.senderID = senderID
        this.time = time
        this.messageKey = messageKey
        this.read= read
        this.type = type
        this.source = source
        this.forward = forward
        this.name = name




    }
}