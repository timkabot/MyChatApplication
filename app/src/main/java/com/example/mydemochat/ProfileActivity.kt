package com.example.mydemochat

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_profile.*
import java.text.DateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var mUserDatabase: DatabaseReference
    private lateinit var mFriendRequestDatabase: DatabaseReference
    private lateinit var mFriendsDatabase : DatabaseReference
    private lateinit var progressDialog: ProgressDialog
    private lateinit var currentState: String
    private lateinit var currentUser: FirebaseUser
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val userId = intent.extras?.get("userId")
        currentState = "not_friends"
        currentUser = FirebaseAuth.getInstance().currentUser!!

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Loading User Data")
        progressDialog.setMessage("Please wait while we load user data")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()

        mFriendRequestDatabase = FirebaseDatabase.getInstance().reference.child("Friend_req")
        mUserDatabase =
            FirebaseDatabase.getInstance().reference.child("Users").child(userId as String)

        mFriendsDatabase = FirebaseDatabase.getInstance().reference.child("Friends")

        val valueEventListener = object : ValueEventListener {
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

                val valueEventListener = object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if (p0.hasChild(userId)) {
                            val req_type = p0.child(userId).child("request_type").value.toString()

                            if(req_type == "received"){
                                currentState = "req_received"
                                profile_send_req_btn.text = "Accept Friend Request"
                            }

                            else if(req_type == "sent"){
                                currentState = "req_sent"
                                profile_send_req_btn.text = "Cancel Friend Request"
                            }
                            progressDialog.dismiss()

                        }

                        else {
                            mFriendsDatabase.child(currentUser.uid).addListenerForSingleValueEvent(
                                object : ValueEventListener{
                                    override fun onCancelled(p0: DatabaseError) {}
                                    override fun onDataChange(p0: DataSnapshot) {
                                        if(p0.hasChild(userId)){
                                            currentState = "friends"
                                            profile_send_req_btn.text = "Unfriend this person"
                                        }
                                    }

                                }
                            )
                            progressDialog.dismiss()

                        }
                    }
                }
                mFriendRequestDatabase.child(currentUser.uid).addListenerForSingleValueEvent(valueEventListener)

            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
            }
        }

        mUserDatabase.addValueEventListener(valueEventListener)

        profile_send_req_btn.setOnClickListener {

            profile_send_req_btn.isEnabled = false

            if(currentState == "friends"){
                mFriendsDatabase
                    .child(currentUser.uid)
                    .child(userId)
                    .removeValue().addOnSuccessListener {
                        mFriendsDatabase.child(userId).child(currentUser.uid).removeValue()
                            .addOnSuccessListener {
                                profile_send_req_btn.isEnabled = true
                                currentState = "not_friends"
                                profile_send_req_btn.text = "Send Friend Request"
                            }
                    }
            }

            // NOT FRIENDS STATE
            if (currentState == "not_friends") {
                mFriendRequestDatabase.child(currentUser.uid).child(userId).child("request_type")
                    .setValue("sent")
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            mFriendRequestDatabase.child(userId)
                                .child(currentUser.uid)
                                .child("request_type")
                                .setValue("received")
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        currentState = "req_sent"
                                        profile_send_req_btn.text = "Cancel Friend Request"
                                        Toast.makeText(
                                            this,
                                            "Request sent succesfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
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
                mFriendRequestDatabase
                    .child(currentUser.uid)
                    .child(userId)
                    .removeValue().addOnSuccessListener {
                        mFriendRequestDatabase.child(userId).child(currentUser.uid).removeValue()
                            .addOnSuccessListener {
                                profile_send_req_btn.isEnabled = true
                                currentState = "not_friends"
                                profile_send_req_btn.text = "Send Friend Request"
                            }
                    }
            }


            //REQUEST RECEIVED STATE
            if(currentState == "req_received"){

                val currentDate = DateFormat.getDateTimeInstance().format(Date())

                mFriendsDatabase.child(currentUser.uid).child(userId).setValue(currentDate)
                    .addOnSuccessListener {
                        mFriendsDatabase.child(userId).child(currentUser.uid).setValue(currentDate)
                            .addOnSuccessListener {
                                mFriendRequestDatabase
                                    .child(currentUser.uid)
                                    .child(userId)
                                    .removeValue().addOnSuccessListener {
                                        mFriendRequestDatabase.child(userId).child(currentUser.uid).removeValue()
                                            .addOnSuccessListener {
                                                profile_send_req_btn.isEnabled = true
                                                currentState = "friends"
                                                profile_send_req_btn.text = "Unfriend this person"
                                            }
                                    }
                            }
                    }
            }
        }
    }
}
