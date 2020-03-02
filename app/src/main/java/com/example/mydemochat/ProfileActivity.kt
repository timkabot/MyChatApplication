package com.example.mydemochat

import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_profile.*
import java.text.DateFormat
import java.util.*
import kotlin.collections.HashMap

class ProfileActivity : AppCompatActivity() {

    private lateinit var mUserDatabase: DatabaseReference
    private lateinit var mFriendRequestDatabase: DatabaseReference
    private lateinit var mFriendsDatabase : DatabaseReference
    private lateinit var mNotificationDatabase: DatabaseReference

    private lateinit var progressDialog: ProgressDialog
    private lateinit var currentState: String
    private lateinit var currentUser: FirebaseUser


    private val currentDatabase : DatabaseReference
        get() = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val userId = intent.extras?.get("userId") as String

        currentState = "not_friends"
        currentUser = FirebaseAuth.getInstance().currentUser!!


        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Loading User Data")
        progressDialog.setMessage("Please wait while we load user data")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()


        initDatabaseReferences(userId)


        val userEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                // Get Post object and use the values to update the UI
                val name = dataSnapshot.child("name").value.toString()
                val status = dataSnapshot.child("status").value.toString()
                val image = dataSnapshot.child("image").value.toString()

                profile_displayName.text = name
                profile_status.text = status

                if (image != "default")
                    Picasso.get().load(image)
                        .placeholder(R.drawable.default_avatar)
                        .into(profile_image)

                val friendRequestEventListener = object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {}

                    override fun onDataChange(p0: DataSnapshot) {
                        if (p0.hasChild(userId)) {
                            val req_type = p0.child(userId).child("request_type").value.toString()

                            if(req_type == "received"){
                                currentState = "req_received"
                                profile_send_req_btn.text = "Accept Friend Request"

                                profile_decline_btn.visibility = View.VISIBLE
                                profile_decline_btn.isEnabled = true
                            }

                            else if(req_type == "sent"){
                                currentState = "req_sent"
                                profile_send_req_btn.text = "Cancel Friend Request"

                                hideDeclineButton()
                            }
                            progressDialog.dismiss()
                        }

                        else {

                            mFriendsDatabase
                                .child(currentUser.uid)
                                .addListenerForSingleValueEvent(
                                object : ValueEventListener{
                                    override fun onCancelled(p0: DatabaseError) {}
                                    override fun onDataChange(p0: DataSnapshot) {
                                        if(p0.hasChild(userId)){
                                            currentState = "friends"
                                            profile_send_req_btn.text = "Unfriend this person"


                                            hideDeclineButton()
                                        }
                                    }

                                }
                            )
                            progressDialog.dismiss()

                        }
                    }
                }
                mFriendRequestDatabase.child(currentUser.uid).addListenerForSingleValueEvent(friendRequestEventListener)

            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
            }
        }

        mUserDatabase.addValueEventListener(userEventListener)


        profile_send_req_btn.setOnClickListener {

            profile_send_req_btn.isEnabled = false

            if(currentState == "friends"){ //unfriend

                unFriend(currentUser.uid,userId)
                    .addOnSuccessListener {
                        unFriend(userId, currentUser.uid)
                            .addOnSuccessListener {
                                profile_send_req_btn.isEnabled = true
                                currentState = "not_friends"

                                profile_send_req_btn.text = "Send Friend Request"
                                hideDeclineButton()
                            }
                    }
            }

            // NOT FRIENDS STATE
            if (currentState == "not_friends") { //CREATE FRIEND REQUEST
                createFriendRequest(currentUser.uid, userId, "sent")
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            createFriendRequest(userId, currentUser.uid,"received")
                                .addOnSuccessListener {
                                        //friend request sent and recieved
                                        val notificationData = HashMap<String, String>()
                                        notificationData["from"] = currentUser.uid
                                        notificationData["type"] = "request"

                                        mNotificationDatabase
                                            .child(userId)
                                            .push()
                                            .setValue(notificationData)
                                            .addOnSuccessListener {
                                                currentState = "req_sent"
                                                profile_send_req_btn.text = "Cancel Friend Request"
                                                hideDeclineButton()
                                            }
                                }
                        } else {
                            Toast.makeText(this, "Failed sending Request", Toast.LENGTH_SHORT)
                                .show()
                        }
                        profile_send_req_btn.isEnabled = true

                    }
            }

            //CANCEL REQUEST STATE
            if (currentState == "req_sent") {
                removeFriendRequest(currentUser.uid, userId)
                    .addOnSuccessListener {
                        removeFriendRequest(userId, currentUser.uid)
                            .addOnSuccessListener {
                                profile_send_req_btn.isEnabled = true
                                currentState = "not_friends"
                                profile_send_req_btn.text = "Send Friend Request"
                                hideDeclineButton()
                            }
                    }
            }


            //REQUEST RECEIVED STATE
            if(currentState == "req_received"){

                val currentDate = DateFormat.getDateTimeInstance().format(Date())

                addFriend(currentUser.uid, userId, currentDate)
                    .addOnSuccessListener {
                        addFriend(userId, currentUser.uid, currentDate)
                            .addOnSuccessListener {
                                removeFriendRequest(currentUser.uid, userId)
                                    .addOnSuccessListener {
                                        removeFriendRequest(userId, currentUser.uid)
                                            .addOnSuccessListener {
                                                currentState = "friends"
                                                profile_send_req_btn.text = "Unfriend this person"
                                                profile_send_req_btn.isEnabled = true
                                                hideDeclineButton()
                                            }
                                    }
                            }
                    }
            }
        }
    }

    private fun unFriend(userA : String, userB : String) = mFriendsDatabase
        .child(userA)
        .child(userB)
        .removeValue()

    private fun createFriendRequest(userA : String, userB : String, request_type : String) = mFriendRequestDatabase
        .child(userA)
        .child(userB )
        .child("request_type")
        .setValue(request_type)

    private fun removeFriendRequest(userA : String, userB : String) = mFriendRequestDatabase
        .child(userA)
        .child(userB)
        .removeValue()

    private fun addFriend(userA : String, userB : String, currentDate : String) = mFriendsDatabase
        .child(userA)
        .child(userB)
        .setValue(currentDate)

    private fun hideDeclineButton() {
        profile_decline_btn.visibility = View.INVISIBLE
        profile_decline_btn.isEnabled = false
    }

    private fun initDatabaseReferences(userId: String) {
        //Init tables
        mFriendRequestDatabase = currentDatabase.child("Friend_req")
        mUserDatabase = currentDatabase.child("Users").child(userId)
        mFriendsDatabase = currentDatabase.child("Friends")
        mNotificationDatabase = currentDatabase.child("Notifications")
    }

}
