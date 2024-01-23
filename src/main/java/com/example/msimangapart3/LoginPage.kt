package com.example.msimangapart3

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class LoginPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        val loginButton = findViewById<Button>(R.id.login_btn)
        val usernameEditText = findViewById<EditText>(R.id.username_login)
        val passwordEditText = findViewById<EditText>(R.id.password_login)
// Make sure to initialize FirebaseApp with the correct context
       // FirebaseApp.initializeApp(this)

        auth = FirebaseAuth.getInstance()

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isEmpty()) {
                usernameEditText.error = "Username is required"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordEditText.error = "Password is required"
                return@setOnClickListener
            }

            loginDatabase(username, password)
        }

        val createAccountTextView = findViewById<TextView>(R.id.create_account_btn)

        createAccountTextView.setOnClickListener {
            val intent = Intent(this, SignUpPage::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loginDatabase(username: String, password: String) {
        auth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivityMap::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}