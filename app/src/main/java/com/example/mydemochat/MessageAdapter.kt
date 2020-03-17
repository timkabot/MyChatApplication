package com.example.mydemochat

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView


class MessageAdapter(private val mMessageList: List<Messages>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    private lateinit var mAuth: FirebaseAuth
    private var mUserDatabase: DatabaseReference? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.message_single_layout, parent, false)

        mAuth = FirebaseAuth.getInstance()
        return MessageViewHolder(v)
    }



    override fun onBindViewHolder(viewHolder: MessageViewHolder, i: Int) {
        val (message, _, _, message_type, from_user) = mMessageList[i]
        val currentUserId = mAuth.currentUser!!.uid

//        if(from_user == currentUserId){
//            viewHolder.messageText.setBackgroundColor(Color.WHITE)
//            viewHolder.messageText.setTextColor(Color.BLACK)
//        }
//        else {
//            viewHolder.messageText.setBackgroundResource(R.drawable.message_text_background)
//            viewHolder.messageText.setTextColor(Color.WHITE)
//        }

        mUserDatabase = FirebaseDatabase.getInstance().reference.child("Users").child(from_user)

        mUserDatabase!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val name = dataSnapshot.child("name").value.toString()
                val image = dataSnapshot.child("thumb_image").value.toString()
                viewHolder.displayName.text = name
                Picasso.get().load(image)
                    .placeholder(R.drawable.default_avatar).into(viewHolder.profileImage)
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })

        if (message_type == "text") {
            viewHolder.messageText.text = message
            viewHolder.messageImage.visibility = View.INVISIBLE
        } else {
            viewHolder.messageText.visibility = View.INVISIBLE
            Picasso.get().load(message)
                .placeholder(R.drawable.default_avatar)
                .into(viewHolder.messageImage)
        }
    }

    override fun getItemCount(): Int {
        return mMessageList.size
    }

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var messageText: TextView = view.findViewById(R.id.message_text_layout)
        var profileImage: CircleImageView = view.findViewById(R.id.message_profile_layout)
        var displayName: TextView = view.findViewById(R.id.name_text_layout)
        var messageImage: ImageView = view.findViewById(R.id.message_image_layout) as ImageView
    }
}