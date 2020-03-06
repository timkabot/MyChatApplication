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
    private lateinit var mFriendsDatabase: DatabaseReference
    private lateinit var mNotificationDatabase: DatabaseReference
    private lateinit var mRootDatabase: DatabaseReference

    private lateinit var progressDialog: ProgressDialog
    private lateinit var currentState: String
    private lateinit var currentUser: FirebaseUser


    private val currentDatabase: DatabaseReference
        get() = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val userId = intent.extras?.get("userId") as String

        currentState = "not_friends"
        currentUser = FirebaseAuth.getInstance().currentUser!!

        progressDialog = ProgressDialog(this)

        showProgressDialog("Loading User Data", "Please wait while we load user data")
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
                            when (p0.child(userId).child("request_type").value.toString()) {
                                "received" -> {
                                    currentState = "req_received"
                                    profile_send_req_btn.text = "Accept Friend Request"

                                    showDeclineButton()
                                }
                                "sent" -> {
                                    currentState = "req_sent"
                                    profile_send_req_btn.text = "Cancel Friend Request"

                                    hideDeclineButton()
                                }
                            }
                            progressDialog.dismiss()
                        } else {
                            mFriendsDatabase
                                .child(currentUser.uid)
                                .addListenerForSingleValueEvent(
                                    object : ValueEventListener {
                                        override fun onCancelled(p0: DatabaseError) = Unit
                                        override fun onDataChange(p0: DataSnapshot) {
                                            if (p0.hasChild(userId)) {
                                                currentState = "friends"
                                                profile_send_req_btn.text = "Unfriend this person"
                                                hideDeclineButton()
                                            }
                                        }
                                    }
                                )

                            hideDeclineButton()
                            progressDialog.dismiss()

                        }
                    }
                }
                mFriendRequestDatabase.child(currentUser.uid)
                    .addListenerForSingleValueEvent(friendRequestEventListener)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
            }
        }

        mUserDatabase.addValueEventListener(userEventListener)

        profile_send_req_btn.setOnClickListener {

            profile_send_req_btn.isEnabled = false

            when (currentState) {
                "friends" -> unFriend(userId)
                "not_friends" -> createFriendRequestWithNotification(currentUser.uid, userId)
                "req_sent" -> removeFriendRequest(currentUser.uid, userId)
                "req_received" -> addFriend(userId)
            }
        }
        profile_decline_btn.setOnClickListener {
            declineFriendRequest(userId)
        }
    }

    private fun unFriend(userId: String) {
        val unFriendMap = HashMap<String, Any?>()
        unFriendMap["Friends/" + currentUser.uid + "/" + userId] = null
        unFriendMap["Friends/" + userId + "/" + currentUser.uid] = null

        mRootDatabase.updateChildren(unFriendMap) { p0, _ ->
            if (p0 == null) {
                profile_send_req_btn.isEnabled = true
                currentState = "not_friends"

                profile_send_req_btn.text = "Send Friend Request"
                hideDeclineButton()
            }
            else Toast.makeText(this, p0.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun declineFriendRequest(userId: String) {
        val unFriendMap = HashMap<String, Any?>()
        unFriendMap["Friend_req/" + currentUser.uid + "/" + userId] = null
        unFriendMap["Friend_req/" + userId + "/" + currentUser.uid] = null

        mRootDatabase.updateChildren(unFriendMap) { p0, _ ->
            if (p0 == null) {
                profile_send_req_btn.isEnabled = true
                currentState = "not_friends"

                profile_send_req_btn.text = "Send Friend Request"
                hideDeclineButton()
            }
            else Toast.makeText(this, p0.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createFriendRequestWithNotification(userA: String, userB: String) {
        val requestMap = HashMap<String, Any>()
        requestMap["Friend_req/$userA/$userB/request_type"] = "sent"
        requestMap["Friend_req/$userB/$userA/request_type"] = "received"

        val (notificationData, newNotificationId) = createNotificationAboutFriendRequest(userB)
        requestMap["Notifications/$userB/$newNotificationId"] = notificationData
        mRootDatabase.updateChildren(requestMap) { _, _ ->
            currentState = "req_sent";
            profile_send_req_btn.text = "Cancel Friend Request"
            hideDeclineButton()
            profile_send_req_btn.isEnabled = true
        }
    }

    private fun removeFriendRequest(userA: String, userB: String) {
        val requestMap = HashMap<String, Any?>()
        requestMap["Friend_req/$userA/$userB/"] = null
        requestMap["Friend_req/$userB/$userA/"] = null
        mRootDatabase.updateChildren(requestMap) { _, _ ->
            profile_send_req_btn.isEnabled = true
            currentState = "not_friends"
            profile_send_req_btn.text = "Send Friend Request"
            hideDeclineButton()
        }
    }

    private fun addFriend(userId: String) {
        val currentDate = DateFormat.getDateTimeInstance().format(Date())
        val friendsMap = HashMap<String, Any?>()
        friendsMap["Friends/" + currentUser.uid + "/" + userId + "/date"] = currentDate
        friendsMap["Friends/" + userId + "/" + currentUser.uid + "/date"] = currentDate

        friendsMap["Friend_req/" + currentUser.uid + "/" + userId] = null
        friendsMap["Friend_req/" + userId + "/" + currentUser.uid ] = null

        mRootDatabase.updateChildren(friendsMap) { p0, _ ->
            if (p0 == null) {
                currentState = "friends"
                profile_send_req_btn.text = "Unfriend this person"

                profile_send_req_btn.isEnabled = true
                hideDeclineButton()
        } else Toast.makeText(this, p0.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideDeclineButton() {
        profile_decline_btn.visibility = View.INVISIBLE
        profile_decline_btn.isEnabled = false
    }

    private fun showDeclineButton() {
        profile_decline_btn.visibility = View.VISIBLE
        profile_decline_btn.isEnabled = true
    }

    private fun initDatabaseReferences(userId: String) {
        //Init tables
        mFriendRequestDatabase = currentDatabase.child("Friend_req")
        mUserDatabase = currentDatabase.child("Users").child(userId)
        mFriendsDatabase = currentDatabase.child("Friends")
        mNotificationDatabase = currentDatabase.child("Notifications")
        mRootDatabase = FirebaseDatabase.getInstance().reference
        hideDeclineButton()
    }

    private fun showProgressDialog(title: String, message: String) {
        progressDialog.setTitle(title)
        progressDialog.setMessage(message)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()
    }

    private fun createNotificationAboutFriendRequest(userTo: String): Pair<HashMap<String, String>, String?> {
        val notificationData = hashMapOf("from" to currentUser.uid, "type" to "request")
        val newNotificationRef = mRootDatabase.child("Notifications").child(userTo).push()
        val newNotificationId = newNotificationRef.key
        return Pair(notificationData, newNotificationId)
    }

    override fun onStart() {
        super.onStart()
        currentUser?.let {
            mUserDatabase.child("online").setValue(true)
        }

    }
    override fun onStop() {
        super.onStop()
        currentUser?.let {
            mUserDatabase.child("online").setValue(false)
        }
    }
}
