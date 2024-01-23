package com.example.msimangapart3

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID

class SaveBird : AppCompatActivity() {
    private lateinit var nameEditText: EditText
    private lateinit var speciesEditText: EditText
    private lateinit var imageView: ImageView

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference

    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null
    private lateinit var pickImage: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_bird)

        nameEditText = findViewById(R.id.editTextTextPersonName)
        speciesEditText = findViewById(R.id.editTextTextPersonName2)

        imageView = findViewById(R.id.imageView)
        pickImage = findViewById(R.id.addImage)

        val saveButton = findViewById<Button>(R.id.saveBtn)

        databaseReference = database.reference
        storageReference = storage.reference

        pickImage.setOnClickListener {
            openGallery()
        }

        saveButton.setOnClickListener {
            // Check if the EditText fields are empty
            val nameIsEmpty = nameEditText.text.toString().isEmpty()
            val speciesIsEmpty = speciesEditText.text.toString().isEmpty()

            val name = nameEditText.text.toString()
            val species = speciesEditText.text.toString()

            if (nameIsEmpty || speciesIsEmpty || imageUri == null) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                // Upload the image to Firebase Storage
                uploadImageToStorage(name, species)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
        Toast.makeText(this, "Image picked successfully", Toast.LENGTH_SHORT).show()
    }

    private fun uploadImageToStorage(name: String, species: String) {
        // Create a reference to "images" in Firebase Storage with a unique name
        val imageName = UUID.randomUUID().toString()
        val imageRef = storageReference.child("images/$imageName")

        // Upload the image to Firebase Storage
        imageRef.putFile(imageUri!!)
            .addOnSuccessListener {
                // Image uploaded successfully, get the download URL
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Save bird data to the Realtime Database
                    saveBirdToDatabase(name, species, uri.toString())
                }
            }
            .addOnFailureListener {
                // Handle unsuccessful image upload
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
    }
    private fun saveBirdToDatabase(name: String, species: String, imageUrl: String) {
        // Create a reference to the "birds" node in the database
        val birdsReference = databaseReference.child("birds")

        // Create a new child node with a unique key and set the bird data
        val newBirdReference = birdsReference.push()
        newBirdReference.child("name").setValue(name)
        newBirdReference.child("species").setValue(species)
        newBirdReference.child("imageUrl").setValue(imageUrl)

        Toast.makeText(this, "Bird saved to database", Toast.LENGTH_SHORT).show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            // Check if imageUri is null before setting it
            if (imageUri == null) {
                imageUri = data.data
                imageView.setImageURI(imageUri)
            } else {
                Toast.makeText(this, "Image already selected", Toast.LENGTH_SHORT).show()
            }
        }
    }


}