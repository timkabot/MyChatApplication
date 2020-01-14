package com.example.mydemochat

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage
import id.zelory.compressor.Compressor
import kotlinx.android.synthetic.main.activity_setting.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception


class SettingsActivity : AppCompatActivity() {
    private lateinit var mUserDatabase: DatabaseReference
    private lateinit var mCurrentUser: FirebaseUser
    private val GALLERY_PICK = 1
    private lateinit var storageFirebase: StorageReference
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        progressDialog = ProgressDialog(this)
        mCurrentUser = FirebaseAuth.getInstance().currentUser!!
        val uid = mCurrentUser.uid
        mUserDatabase = FirebaseDatabase.getInstance().reference.child("Users").child(uid)
        mUserDatabase.keepSynced(true)

        mUserDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val name = dataSnapshot.child("name").value.toString()
                val image = dataSnapshot.child("image").value.toString()
                val status = dataSnapshot.child("status").value.toString()
                val thumbImage = dataSnapshot.child("thumb_image").value.toString()

                nameTextView.text = name
                statusTextView.text = status
                if (image != "defaul"){
                    Picasso.get().load(image)
                        .networkPolicy(NetworkPolicy.OFFLINE)
                        .placeholder(R.drawable.default_avatar)
                        .into(circleImageView, object : Callback{
                            override fun onSuccess() {
                            }

                            override fun onError(e: Exception?) {
                                Picasso.get().load(image).placeholder(R.drawable.default_avatar).into(circleImageView)
                            }
                        })
                }
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
                startActivityForResult(
                    Intent.createChooser(galleryIntent, "Select image"),
                    GALLERY_PICK
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK)
            when (requestCode) {

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
                    val thumbFile = File(resultUri.path)
                    val current_user_id = mCurrentUser.uid

                    val compressedImageBitmap = Compressor(this)
                        .setMaxHeight(200)
                        .setMaxWidth(200)
                        .setQuality(75)
                        .compressToBitmap(thumbFile)

                    val baos = ByteArrayOutputStream()
                    compressedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val thumb_byte = baos.toByteArray()

                    storageFirebase = FirebaseStorage.getInstance().reference

                    val filepath: StorageReference = storageFirebase.child("profile_images")
                        .child("${current_user_id}.jpg")
                    val thumb_filepath_in_database = storageFirebase.child("profile_images").child("thumbs")
                        .child("${current_user_id}.jpg")


                    filepath.putFile(resultUri).addOnCompleteListener { addImageTask ->
                        if (addImageTask.isSuccessful) {
                            filepath.downloadUrl.addOnCompleteListener { getDownloadLinkTask ->
                                if (getDownloadLinkTask.isSuccessful) {
                                    //uploadthumbnail
                                    thumb_filepath_in_database.putBytes(thumb_byte).addOnCompleteListener { addThumbnailTask ->
                                        if(addThumbnailTask.isSuccessful){
                                            thumb_filepath_in_database.downloadUrl.addOnCompleteListener {getThumbnailLinkTask ->
                                                if(getThumbnailLinkTask.isSuccessful){
                                                    val thumbNailDownloadLink = getThumbnailLinkTask.result.toString()
                                                    val downloadLink = getDownloadLinkTask.result.toString()
                                                    val updateHashMap : MutableMap<String,String> = HashMap()
                                                    updateHashMap["image"] = downloadLink
                                                    updateHashMap["thumb_image"] = thumbNailDownloadLink

                                                    mUserDatabase.updateChildren(updateHashMap as Map<String, Any>)
                                                        .addOnCompleteListener {
                                                            if (it.isSuccessful) {
                                                                progressDialog.dismiss()
                                                                Toast.makeText(
                                                                    this,
                                                                    "Update succesfull",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                            }
                                                        }
                                                }
                                                else {
                                                    Toast.makeText(this, "Error in loading thumbnail download link", Toast.LENGTH_LONG).show()
                                                    progressDialog.dismiss()
                                                }
                                            }
                                        }
                                        else {
                                            Toast.makeText(this, "Error in thumbnail", Toast.LENGTH_LONG).show()
                                            progressDialog.dismiss()

                                        }
                                    }

                                }
                            }

                        } else {
                            Toast.makeText(this, "Error in uploading", Toast.LENGTH_LONG).show()
                            progressDialog.dismiss()
                        }

                    }
                }
            }
    }
}
