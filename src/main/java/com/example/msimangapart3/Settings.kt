package com.example.msimangapart3

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Settings : AppCompatActivity() {

    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var maxDistance: String
    private lateinit var maxDate: String
    private lateinit var metric: String
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize Firebase Database reference
        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = database.reference

        var metrics = findViewById<Spinner>(R.id.spinner)

        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("kilometres", "miles"))
        metrics.adapter = adapter

        var saveButton = findViewById<Button>(R.id.saveBtn)
        metric = "kilometres"
        saveButton.setOnClickListener {

            metrics.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    metric = parent?.getItemAtPosition(position).toString()
                    // You can use "selectedValue" here or in other parts of your code
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Handle the case where nothing is selected (if needed)
                }
            }

            // Move these lines inside the click listener
            val maxDistance = findViewById<EditText>(R.id.maxDistance).text.toString()
            val maxDate = findViewById<EditText>(R.id.daysMax).text.toString()

            // Save settings to Firebase Realtime Database
            saveSettingsToFirebase(metric, maxDistance, maxDate)

            Toast.makeText(this, "Settings updated", Toast.LENGTH_SHORT).show()

            finish()
        }
    }

    private fun saveSettingsToFirebase(metric: String, maxDistance: String, maxDate: String) {
        // Assuming you have a "settings" node in your database
        val settingsReference = databaseReference.child("Settings")

        // Save settings data
        settingsReference.child("metric").setValue(metric)
        settingsReference.child("maxDistance").setValue(maxDistance)
        settingsReference.child("maxDate").setValue(maxDate)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}