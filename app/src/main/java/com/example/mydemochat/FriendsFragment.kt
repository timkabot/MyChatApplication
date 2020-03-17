package com.example.mydemochat

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.card_user.view.*
import kotlinx.android.synthetic.main.fragment_friends.*


class FriendsFragment : Fragment() {

    private lateinit var mFriendsDatabase: DatabaseReference
    lateinit var mUsersDatabase: DatabaseReference

    lateinit var mAuth: FirebaseAuth
    lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mAuth = FirebaseAuth.getInstance()
        currentUserId = mAuth.currentUser!!.uid
        mFriendsDatabase = FirebaseDatabase.getInstance().reference.child("Friends").child(currentUserId)
        mFriendsDatabase.keepSynced(true)

        mUsersDatabase = FirebaseDatabase.getInstance().reference.child("Users")
        mUsersDatabase.keepSynced(true)



        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_friends, container, false)
    }

    override fun onStart() {
        super.onStart()
        friends_list.layoutManager = (LinearLayoutManager(context))
        friends_list.setHasFixedSize(true)
        val friendsQuery =
            mFriendsDatabase.limitToLast(50)

        val options = FirebaseRecyclerOptions.Builder<Friends>()
            .setQuery(friendsQuery, Friends::class.java)
            .setLifecycleOwner(this)
            .build()

        val firebaseRecyclerAdapter = object : FirebaseRecyclerAdapter<Friends, FriendsViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendsViewHolder {
                return FriendsViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.card_user, parent, false)
                )
            }

            override fun onBindViewHolder(holder: FriendsViewHolder, position: Int, model: Friends) {
                holder.bind(model)
                val userId : String? = getRef(position).key
                userId?.let {
                    mUsersDatabase.child(it).addValueEventListener(object :ValueEventListener{
                        override fun onCancelled(p0: DatabaseError) {
                        }
                        override fun onDataChange(p0: DataSnapshot) {
                            val username = p0.child("name").value.toString()
                            val status = p0.child("status").value.toString()
                            val thumbImage = p0.child("thumb_image").value.toString()
                            val userOnline = p0.child("online").value.toString()
                            holder.setUserOnline(userOnline)
                            holder.bind(User(name = username, status = status, thumb_image = thumbImage))

                            holder.itemView.setOnClickListener {
                                val dialogOptions : Array<CharSequence> = arrayOf("Open Profile","Send message")

                                val builder = AlertDialog.Builder(context)
                                builder.setTitle("Select options")
                                builder.setItems(dialogOptions
                                ) { _, i ->
                                    when(i) {
                                        0 -> {
                                            val profileIntent = Intent(context, ProfileActivity::class.java)
                                            profileIntent.putExtra("userId", userId)
                                            startActivity(profileIntent)
                                        }
                                        1 -> {
                                            val chatIntent = Intent(context, ChatActivity::class.java)
                                            chatIntent.putExtra("userId", userId)
                                            chatIntent.putExtra("userName", username)
                                            startActivity(chatIntent)
                                        }
                                    }
                                }
                                builder.show()
                            }
                        }
                    })
                }
            }
        }
        friends_list.adapter = firebaseRecyclerAdapter;

    }

    class FriendsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item : Friends){
            itemView.user_single_status.text = item.date
        }

        fun bind(item : User){
            itemView.user_single_status.text = item.status
            itemView.user_single_name.text = item.name
            if (item.thumb_image != "default")
                Picasso.get().load(item.thumb_image)
                    .placeholder(R.drawable.default_avatar)
                    .into(itemView.user_single_image)
        }

        fun setUserOnline(online_status : String){
            when(online_status) {
                "true" ->   itemView.user_single_online_icon.visibility = View.VISIBLE
                "false" ->  itemView.user_single_online_icon.visibility = View.INVISIBLE
            }

        }
    }

}

