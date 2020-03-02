package com.example.mydemochat

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_register.*


class RegisterActivity : AppCompatActivity() {

    private var mAuth: FirebaseAuth? = null
    lateinit var mRegProgress: ProgressDialog
    private lateinit var mDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        mAuth = FirebaseAuth.getInstance()

        mRegProgress = ProgressDialog(this)

        create_account_button.setOnClickListener {
            val displayName = nameInputLayout.editText?.text.toString()
            val email = emailInputLayout.editText?.text.toString()
            val password = passwordInputLayout.editText?.text.toString()

            if (!TextUtils.isEmpty(displayName) &&
                !TextUtils.isEmpty(email) &&
                !TextUtils.isEmpty(password)
            ) {
                mRegProgress.setTitle("Registering User")
                mRegProgress.setMessage("Please wait while we create your account")
                mRegProgress.setCanceledOnTouchOutside(false)
                mRegProgress.show()
                registerUser(displayName, email, password)
            }

        }


        setSupportActionBar(register_toolbar as Toolbar?)
        supportActionBar?.title = "Create account"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun registerUser(displayName: String, email: String, password: String) {
        mAuth?.apply {
            createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        currentUser?.uid?.let { uid ->
                            println(uid)
                            mDatabase =
                                FirebaseDatabase.getInstance().reference.child("Users").child(uid)
                        }
                        val deviceToken : String = FirebaseInstanceId.getInstance().token.toString()

                        val userMap = HashMap<String, String>()
                        userMap["name"] = displayName
                        userMap["status"] = getString(R.string.default_status)
                        userMap["image"] = "default"
                        userMap["thumb_image"] = "default"
                        userMap["device_token"] = deviceToken

                        mDatabase.setValue(userMap).addOnCompleteListener {
                            if (it.isSuccessful) {
                                mRegProgress.dismiss()
                                val mainIntent =
                                    Intent(this@RegisterActivity, MainActivity::class.java)
                                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(mainIntent)
                                finish()
                            }
                        }

                    } else {
                        mRegProgress.hide()
                        Toast.makeText(
                            applicationContext,
                            "Please check form and try again",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}
