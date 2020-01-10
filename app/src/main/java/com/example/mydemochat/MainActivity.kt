package com.example.mydemochat

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var mAuth: FirebaseAuth? = null
    private lateinit var mSectionsPagerAdapter : SectionsPagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mAuth = FirebaseAuth.getInstance()

        setSupportActionBar(main_page_toolbar as Toolbar?)
        supportActionBar?.title = "Timur Chat"

        mSectionsPagerAdapter = SectionsPagesAdapter(fragmentManager = supportFragmentManager)

        tabPager.adapter = mSectionsPagerAdapter

        tabLayout.setupWithViewPager(tabPager)

    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = mAuth!!.currentUser
        //updateUI(currentUser)

        if (currentUser == null) {
           sendToStart()
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
