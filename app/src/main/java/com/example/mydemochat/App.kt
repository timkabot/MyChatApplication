package com.example.mydemochat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso

class App : Application() {

    lateinit var mUserDatabase: DatabaseReference
    lateinit var mAuth: FirebaseAuth
    override fun onCreate() {
        super.onCreate()
        initDatabase()
        val builder = Picasso.Builder(this)
        builder.downloader(OkHttp3Downloader(this, Long.MAX_VALUE))
        val built = builder.build()
        built.setIndicatorsEnabled(true)
        built.isLoggingEnabled = true
        Picasso.setSingletonInstance(built)
        createNotificationChannel()
    }

    private fun initDatabase(){
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        mAuth = FirebaseAuth.getInstance()
        mUserDatabase = FirebaseDatabase.getInstance().reference.child("Users").child(mAuth.currentUser!!.uid)

        mUserDatabase.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                mUserDatabase.child("online").onDisconnect().setValue(false)
            }

        })
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Channel 1"
            val descriptionText = "My notification channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}