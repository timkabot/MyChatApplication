package com.example.mydemochat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_users.*
import kotlinx.android.synthetic.main.card_user.view.*

class UsersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)

        setSupportActionBar(usersToolbar as Toolbar?)
        supportActionBar?.title = "All users"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        usersList.setHasFixedSize(true)
        usersList.layoutManager = LinearLayoutManager(this)
    }

    override fun onStart() {
        super.onStart()

        val usersQuery = FirebaseDatabase.getInstance()
            .reference
            .child("Users")
            .limitToLast(50)

        val options = FirebaseRecyclerOptions.Builder<User>()
            .setQuery(usersQuery, User::class.java)
            .setLifecycleOwner(this)
            .build()

        val firebaseRecyclerAdapter = object : FirebaseRecyclerAdapter<User, UsersViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
                return UsersViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.card_user, parent, false)
                )
            }
            override fun onBindViewHolder(holder: UsersViewHolder, position: Int, model: User) {
                holder.bind(model)

                val userId = getRef(position).key
                holder.itemView.setOnClickListener {
                    val profileIntent = Intent(this@UsersActivity, ProfileActivity::class.java)
                    profileIntent.putExtra("userId", userId)
                    startActivity(profileIntent)
                }
            }
        }

        usersList.adapter = firebaseRecyclerAdapter
    }

    class UsersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(user: User){
            with(user){
                itemView.user_single_name.text = name
                itemView.user_single_status.text = status
                if(thumb_image != "default")
                Picasso.get().load(thumb_image)
                    .placeholder(R.drawable.default_avatar)
                    .into( itemView.user_single_image)
            }
        }
    }

}
