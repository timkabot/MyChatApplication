package com.example.mydemochat

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mSectionsPagerAdapter : SectionsPagesAdapter
    private lateinit var mUserDatabase : DatabaseReference
    private var currentUser : FirebaseUser? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mAuth = FirebaseAuth.getInstance()

        if(mAuth.currentUser != null){

            mUserDatabase = FirebaseDatabase.getInstance().reference.child("Users").child(mAuth.currentUser!!.uid)
        }

        setSupportActionBar(main_page_toolbar as Toolbar?)
        supportActionBar?.title = "Timur Chat"

        mSectionsPagerAdapter = SectionsPagesAdapter(fragmentManager = supportFragmentManager)

        tabPager.adapter = mSectionsPagerAdapter

        tabLayout.setupWithViewPager(tabPager)

    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        currentUser = mAuth.currentUser
        //updateUI(currentUser)

        if (currentUser == null) sendToStart()
        else mUserDatabase.child("online").setValue("true")
    }

    override fun onStop() {
        super.onStop()
        currentUser?.let {
            mUserDatabase.child("online").setValue(ServerValue.TIMESTAMP)
        }
    }
    private fun sendToStart(){
        val startIntent = Intent(this, StartActivity::class.java)
        startActivity(startIntent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)

        when(item.itemId){
            R.id.main_logout_btn -> {
                FirebaseAuth.getInstance().signOut()
                sendToStart()
            }

            R.id.main_settings_button -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)
            }

            R.id.main_all_users_button -> {
                val allUsersIntent = Intent(this, UsersActivity::class.java)
                startActivity(allUsersIntent)
            }
        }

        return true
    }
}
