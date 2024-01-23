package com.example.msimangapart3

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class SignUpPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_page)

        val signUpButton = findViewById<Button>(R.id.sign_up_btn)
        val usernameEditText = findViewById<EditText>(R.id.username_sign_up)
        val passwordEditText = findViewById<EditText>(R.id.password_sign_up)

        auth = FirebaseAuth.getInstance()

        signUpButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username and password are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create a new user in Firebase Authentication
            auth.createUserWithEmailAndPassword(username, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "SignUp successfully", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginPage::class.java))
                        finish()
                    } else {
                        // If sign up fails, display a message to the user.
                        try {
                            throw task.exception!!
                        } catch (e: FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "User with this email already exists", Toast.LENGTH_SHORT).show()
                        } catch (e: FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, "SignUp Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }

        val loginBtn = findViewById<TextView>(R.id.login_text_btn)
        loginBtn.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}