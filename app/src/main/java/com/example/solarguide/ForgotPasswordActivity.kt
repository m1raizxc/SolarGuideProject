package com.example.solarguide

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.solarguide.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var buttonClickPlayer: MediaPlayer
    private lateinit var errorPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        buttonClickPlayer = MediaPlayer.create(this, R.raw.button_click)
        errorPlayer = MediaPlayer.create(this, R.raw.error_sound)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.resetPassword.setOnClickListener {
            val email = binding.forgotPassEmailEt.text.toString().trim()

            if (email.isNotEmpty()) {
                if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Reset link sent to your email.", Toast.LENGTH_SHORT).show()
                            buttonClickPlayer.start()
                        } else {
                            val errorMessage = task.exception?.message ?: "Unable to send reset email."
                            errorPlayer.start()
                            if (task.exception is FirebaseAuthInvalidUserException) {
                                // Handle case where user does not exist
                                Toast.makeText(this, "User does not exist.", Toast.LENGTH_SHORT).show()
                                errorPlayer.start()
                            } else {
                                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                                errorPlayer.start()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Invalid email format.", Toast.LENGTH_SHORT).show()
                    errorPlayer.start()
                }
            } else {
                Toast.makeText(this, "Please enter email.", Toast.LENGTH_SHORT).show()
                errorPlayer.start()
            }
        }

        // Set up the back to login button click listener
        binding.backToLogin.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            buttonClickPlayer.start()
            finish() // Optionally finish this activity if you don't want to keep it in the back stack
        }
    }
}
