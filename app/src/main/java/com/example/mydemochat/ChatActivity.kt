package com.example.mydemochat

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.DatabaseReference
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.chat_custom_bar.*


class ChatActivity : AppCompatActivity() {
    private lateinit var mChatUser: String
    private lateinit var mRootRef: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCurrentUserId: String


    private lateinit var mLinearLayoutManager : LinearLayoutManager
    private lateinit var messageAdapter: MessageAdapter
    private var messagesList = ArrayList<Messages>()

    private val TOTAL_ITEMS_TO_LOAD = 10
    private var mCurrentPage = 1

    private var itemPos = 0
    private var mLastKey = ""
    private var mPrevKey = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        mChatUser = intent.extras?.get("userId") as String

        mRootRef = FirebaseDatabase.getInstance().reference
        mAuth = FirebaseAuth.getInstance()
        mCurrentUserId = mAuth.currentUser?.uid ?: "no user"
        println(mCurrentUserId)
        initBar()
        initChat()
        initRecycler()
        initListeners()
    }

    private fun initBar() {
        setSupportActionBar(chat_app_bar as Toolbar)

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val customView = inflater.inflate(R.layout.chat_custom_bar, null)

        supportActionBar?.let {
            it.setDisplayShowCustomEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
            it.customView = customView
        }

        custom_bar_title.text = intent.extras?.get("userName") as String
        mRootRef.child("Users").child(mChatUser).addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                val online = p0.child("online").value.toString()
                val image = p0.child("image").value.toString()

                if (online == "true") custom_bar_seen.text = "Online"
                else {
                    val timeInteractor = TimeInteractor()
                    custom_bar_seen.text = timeInteractor.getTimeAgo(online.toLong())
                }

                if (image != "default") {

                }
            }

        })
    }
    private fun initRecycler(){
        mLinearLayoutManager = LinearLayoutManager(applicationContext)
        messages_list.setHasFixedSize(true)
        messages_list.layoutManager = mLinearLayoutManager

        messageAdapter = MessageAdapter(messagesList)
        messages_list.adapter =messageAdapter
        loadMessages()
    }
    private fun initChat() {
        mRootRef.child("Chat").child(mCurrentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (!p0.hasChild(mChatUser)) {
                        val chatAddMap = HashMap<String, Any>()
                        chatAddMap["seen"] = false
                        chatAddMap["timestamp"] = ServerValue.TIMESTAMP

                        val chatUserMap = HashMap<String, Any>()
                        chatUserMap["Chat/$mCurrentUserId/$mChatUser"] = chatAddMap
                        chatUserMap["Chat/$mChatUser/$mCurrentUserId"] = chatAddMap

                        mRootRef.updateChildren(chatUserMap)
                    }
                }

            })
    }

    private fun initListeners(){
        chat_send_btn.setOnClickListener {
            sendMessage()
        }

        message_swipe_layout.setOnRefreshListener {
            mCurrentPage++;
            itemPos = 0;
            loadMoreMessages();
        }

    }

    private fun sendMessage() {
        println("We are in sendMessage function")

        val message = chat_message_view.text.toString()
        if(message.isNotEmpty()){
            val messageMap = HashMap<String, Any>()
            val currentUserRef = "Messages/$mCurrentUserId/$mChatUser"
            val chatUserRef = "Messages/$mChatUser/$mCurrentUserId"
            val userMessagePush = mRootRef.child("Messages").child(mCurrentUserId)
                .child(mChatUser).push()
            val pushId = userMessagePush.key
            messageMap["message"] = message
            messageMap["seen"] = false
            messageMap["type"] = "text"
            messageMap["time"] = ServerValue.TIMESTAMP
            messageMap["from"] = mCurrentUserId
            val messageUserMap = HashMap<String, Any>()
            messageUserMap["$currentUserRef/$pushId"] = messageMap
            messageUserMap["$chatUserRef/$pushId"] = messageMap
            chat_message_view.text.clear()

            mRootRef.updateChildren(messageUserMap).addOnCompleteListener {
                DatabaseReference.CompletionListener { p0, _ ->
                    if(p0 != null) Log.d("ds", p0.message)
                }
            }
        }
    }

    private fun loadMessages(){
        val messageRef =
            mRootRef.child("Messages").child(mCurrentUserId).child(mChatUser)
        val messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD)

        messageQuery
            .addChildEventListener(object : ChildEventListener{
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onChildMoved(p0: DataSnapshot, p1: String?) {
                }

                override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                }

                override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                    val message = p0.getValue(Messages::class.java)!!
                    itemPos++

                    if (itemPos == 1) {
                        val messageKey: String = p0.key!!
                        mLastKey = messageKey
                        mPrevKey = messageKey
                    }
                    messagesList.add(message)
                    messageAdapter.notifyDataSetChanged()
                    messages_list.scrollToPosition(messagesList.size - 1);
                    message_swipe_layout.isRefreshing = false
                }

                override fun onChildRemoved(p0: DataSnapshot) {
                }

            })
    }

    private fun loadMoreMessages() {
        val messageRef =
            mRootRef.child("Messages").child(mCurrentUserId).child(mChatUser)
        val messageQuery = messageRef.orderByKey().endAt(mLastKey).limitToLast(10)
        messageQuery.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                val message = dataSnapshot.getValue(Messages::class.java)
                val messageKey = dataSnapshot.key

                if (mPrevKey != messageKey) {
                    messagesList.add(itemPos++, message!!)
                } else {
                    mPrevKey = mLastKey
                }
                if (itemPos == 1) {
                    mLastKey = messageKey!!
                }
                Log.d(
                    "TOTALKEYS",
                    "Last Key : $mLastKey | Prev Key : $mPrevKey | Message Key : $messageKey"
                )
                messageAdapter.notifyDataSetChanged()
                message_swipe_layout.isRefreshing = false
                mLinearLayoutManager.scrollToPositionWithOffset(10, 0)
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }
}