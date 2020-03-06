package com.example.mydemochat

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    lateinit var progressDialog: ProgressDialog
    lateinit var mAuth: FirebaseAuth
    lateinit var mUserDatabase : DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        mAuth = FirebaseAuth.getInstance()

        setSupportActionBar(login_toolbar as Toolbar?)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Login"

        progressDialog = ProgressDialog(this)

        mUserDatabase = FirebaseDatabase.getInstance().reference.child("Users")

        login_account_button.setOnClickListener {

            val email = emailInputLayout.editText?.text.toString()
            val password = passwordInputLayout.editText?.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                progressDialog.setTitle("Loging in")
                progressDialog.setMessage("Please wait while we checl your credentials")
                progressDialog.setCanceledOnTouchOutside(false)
                progressDialog.show()
                loginUser(email, password)
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {

                progressDialog.dismiss()
                val currentUserId = mAuth.currentUser!!.uid
                val deviceToken = FirebaseInstanceId.getInstance().token


                addToken(currentUserId, deviceToken)
                    .addOnSuccessListener {
                        val mainIntent = Intent(this, MainActivity::class.java)
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(mainIntent)
                        finish()
                    }

            } else {
                progressDialog.hide()
                Toast.makeText(
                    applicationContext,
                    "Can't sign in. Please check the form and try again",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun addToken(userId : String, deviceToken: String?) = mUserDatabase
        .child(userId)
        .child("device_token")
        .setValue(deviceToken)
}
