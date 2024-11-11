package com.example.solarguide

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.solarguide.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var buttonClickPlayer: MediaPlayer
    private lateinit var errorPlayer: MediaPlayer
    private lateinit var popPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize MediaPlayer for button click and error sounds
        buttonClickPlayer = MediaPlayer.create(this, R.raw.button_click)
        errorPlayer = MediaPlayer.create(this, R.raw.error_sound)
        popPlayer = MediaPlayer.create(this, R.raw.pop_sound)

        binding.buttonPhoneauth.setOnClickListener {
            val intent = Intent(this, PhoneActivity::class.java)
            startActivity(intent)
            // Play the button press sound
            buttonClickPlayer.start()
        }

        firebaseAuth = FirebaseAuth.getInstance()

        binding.buttonLogin.setOnClickListener {
            // Play the button press sound

            val email = binding.emailEt.text.toString().trim()
            val password = binding.passET.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                signInUser(email, password)
                buttonClickPlayer.start()
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                errorPlayer.start()
            }
        }

        binding.textViewSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            popPlayer.start()
        }

        binding.forgotpass.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
            popPlayer.start()
        }
    }

    private fun signInUser(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = firebaseAuth.currentUser
                if (user != null) {
                    if (user.isEmailVerified) {
                        val intent = Intent(this, WeatherActivity::class.java)
                        startActivity(intent)
                        buttonClickPlayer.start()
                    } else {
                        // User is not verified, sign out and show a message
                        firebaseAuth.signOut()
                        Toast.makeText(this, "Please verify your email before logging in.", Toast.LENGTH_LONG).show()
                        errorPlayer.start()
                    }
                }
            } else {
                // Play error sound and handle errors during sign-in
                errorPlayer.start()
                handleSignInError(task.exception)
            }
        }
    }

    private fun handleSignInError(exception: Exception?) {
        try {
            throw exception ?: Exception("Unknown authentication error")
        } catch (e: FirebaseAuthInvalidUserException) {
            // Play error sound and show email not found message
            errorPlayer.start()
            Toast.makeText(this, "Email not found. Please sign up first.", Toast.LENGTH_SHORT).show()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            // Play error sound and show invalid password message
            errorPlayer.start()
            Toast.makeText(this, "Invalid Credentials. Please try again.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Play error sound and show generic error message
            errorPlayer.start()
            Toast.makeText(this, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("FirebaseAuth", "Authentication failed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release MediaPlayer resources
        buttonClickPlayer.release()
        errorPlayer.release()
    }
}
