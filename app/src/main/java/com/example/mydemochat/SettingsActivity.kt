package com.example.mydemochat

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.activity_setting.*
import java.io.File


class SettingsActivity : AppCompatActivity() {
    private lateinit var mUserDatabase: DatabaseReference
    private lateinit var mCurrentUser: FirebaseUser
    private val GALLERY_PICK = 1
    private lateinit var storageFirebase : StorageReference
    private lateinit var progressDialog : ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        progressDialog = ProgressDialog(this)
        mCurrentUser = FirebaseAuth.getInstance().currentUser!!
        val uid = mCurrentUser.uid
        mUserDatabase = FirebaseDatabase.getInstance().reference.child("Users").child(uid)


        mUserDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val name = dataSnapshot.child("name").value.toString()
                val image = dataSnapshot.child("image").value.toString()
                val status = dataSnapshot.child("status").value.toString()
                val thumbImage = dataSnapshot.child("thumb_image").value.toString()

                nameTextView.text = name
                statusTextView.text = status
                Toast.makeText(applicationContext, dataSnapshot.toString(), Toast.LENGTH_LONG)
                    .show()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(applicationContext, databaseError.toString(), Toast.LENGTH_LONG)
                    .show()

            }
        })


        changeImageButton.setOnClickListener {
            val galleryIntent = Intent()
            with(galleryIntent) {
                type = "image/*"
                action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(galleryIntent, "Select image"), GALLERY_PICK)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK)
            when(requestCode) {

                GALLERY_PICK -> {
                    val imageUri = data?.data
                    CropImage.activity(imageUri)
                        .setAspectRatio(1, 1)
                        .setMinCropWindowSize(500, 500)
                        .start(this)
                }

                CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                    progressDialog.setTitle("Uploading Image")
                    progressDialog.setMessage("Please wait while we upload and process the image")
                    progressDialog.setCanceledOnTouchOutside(false)
                    progressDialog.show()

                    val result = CropImage.getActivityResult(data)
                    val resultUri = result.uri
                    val thumb_filePath = File(resultUri.path)
                    val current_user_id = mCurrentUser.uid


                    storageFirebase = FirebaseStorage.getInstance().reference
                    val filepath : StorageReference = storageFirebase.child("profile_images")
                        .child("${current_user_id}.jpg")

                    filepath.putFile(resultUri).addOnCompleteListener { task ->
                        if(task.isSuccessful) {
                            filepath.downloadUrl.addOnCompleteListener {getDownloadLinkTask ->
                                if(getDownloadLinkTask.isSuccessful){
                                    val downloadLink = getDownloadLinkTask.result.toString()
                                    mUserDatabase.child("image").setValue(downloadLink).addOnCompleteListener {
                                        if(it.isSuccessful){
                                            progressDialog.dismiss()
                                            Toast.makeText(this,"Update succesfull", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }

                        }
                        else
                        {
                            Toast.makeText(this,"Error in uploading", Toast.LENGTH_LONG).show()
                            progressDialog.dismiss()
                        }

                    }
                }
            }
    }
}
