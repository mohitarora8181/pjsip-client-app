package com.mohit.pjsip_client_app

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mizuvoip.jvoip.SIPNotification
import com.mizuvoip.jvoip.SIPNotificationListener
import com.mizuvoip.jvoip.SipStack

object SipManager {

    private var isInitialized = false

    @SuppressLint("StaticFieldLeak")
    private var sipStack: SipStack? = null

    var callStatus by mutableStateOf("Idle")
    var registrationStatus by mutableStateOf("Not Registered")
    var callDuration by mutableStateOf("00:00")
    var isMuted by mutableStateOf(false)
    var isSpeakerOn by mutableStateOf(false)

    private var callStartTime: Long = 0

    var isIncomingCall by mutableStateOf(false)
    var incomingCallNumber by mutableStateOf("")

    fun init(context: Context, server: String, username: String, password: String) {
        if (isInitialized) return

        sipStack = SipStack().apply {
            Init(context)
            SetParameter("serveraddress", server)
            SetParameter("username", username)
            SetParameter("password", password)
            SetParameter("transport", "UDP")
            SetParameter("register", "1")
            SetParameter("enablemicrophone", "1")
            SetParameter("audiotransmit", "1")

            Start()
        }

        sipStack?.SetNotificationListener(MySIPNotificationListener())

        isInitialized = true
        registrationStatus = "Registered"
        configureAudio(context)
    }


    fun getIncomingCaller(): String {
        return sipStack?.GetIncomingDisplay(-1) ?: ""
    }

    fun answerIncomingCall() {
        sipStack?.Accept(-1)
        callStatus = "In Call with $incomingCallNumber"
        isIncomingCall = false
        incomingCallNumber = ""
    }

    fun rejectIncomingCall() {
        sipStack?.Reject(-1)
        callStatus = "Call Rejected"
        isIncomingCall = false
        incomingCallNumber = ""
    }

    private fun configureAudio(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true
        isSpeakerOn = true
    }

    fun toggleSpeaker(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        isSpeakerOn = !isSpeakerOn
        audioManager.isSpeakerphoneOn = isSpeakerOn
    }

    fun toggleMute() {
        isMuted = !isMuted
        sipStack?.Mute(-1,isMuted)
    }

    fun makeCall(destination: String) {
        sipStack?.Call(-1, destination)
        callStatus = "Calling $destination..."
        callStartTime = System.currentTimeMillis()
        Log.d("SIP", "Calling $destination")
    }

    fun hangUp() {
        sipStack?.Hangup(-1)
        callStatus = "Call Ended"
        callDuration = "00:00"
        Log.d("SIP", "Call terminated")
    }

    fun rejectCall() {
        sipStack?.Reject(-1)
        callStatus = "Call Rejected"
        Log.d("SIP", "Call rejected")
    }

    fun holdCall() {
        sipStack?.Hold(-1,true)
        callStatus = "Call on Hold"
        Log.d("SIP", "Call on hold")
    }


    fun releaseCall() {
        sipStack?.Hold(-1,false)
        callStatus = "Call on Release"
        Log.d("SIP", "Call on Release")
    }
}


class MySIPNotificationListener : SIPNotificationListener(){
    override fun onEvent(e: SIPNotification.Event?) {
        super.onEvent(e)
        if(e!!.text.contains("Call duration:")){
            SipManager.rejectCall()
        }
        println("Listener Event : ${e.text}")
    }
}

