package com.example.service
import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle

object ReplyCache {
    val replies = mutableMapOf<String, Notification.Action>()

    fun sendReply(context: Context, key: String, message: String) {
        val action = replies[key] ?: return
        val remoteInputs = action.remoteInputs ?: return
        
        val intent = Intent()
        val bundle = Bundle()
        bundle.putCharSequence(remoteInputs[0].resultKey, message)
        RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
        
        try {
            action.actionIntent.send(context, 0, intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
