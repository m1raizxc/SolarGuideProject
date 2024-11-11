package com.example.solarguide

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.solarguide.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var buttonClickPlayer: MediaPlayer
    private lateinit var errorPlayer: MediaPlayer
    private lateinit var popPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        buttonClickPlayer = MediaPlayer.create(this, R.raw.button_click)
        errorPlayer = MediaPlayer.create(this, R.raw.error_sound)
        popPlayer = MediaPlayer.create(this, R.raw.pop_sound)

        binding.textView.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            popPlayer.start()
        }

        binding.buttonRegister.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val pass = binding.passET.text.toString().trim()
            val confirmPass = binding.confirmPassEt.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Invalid email format.", Toast.LENGTH_SHORT).show()
                    errorPlayer.start()
                    return@setOnClickListener
                }

                if (pass.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                    errorPlayer.start()
                    return@setOnClickListener
                }

                if (pass == confirmPass) {
                    firebaseAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Send email verification
                                firebaseAuth.currentUser?.sendEmailVerification()
                                    ?.addOnCompleteListener { verifyTask ->
                                        if (verifyTask.isSuccessful) {
                                            Toast.makeText(this, "Verification email sent. Please check your email.", Toast.LENGTH_LONG).show()
                                            buttonClickPlayer.start()
                                            // Redirect to sign-in activity
                                            val intent = Intent(this, SignInActivity::class.java)
                                            startActivity(intent)
                                        } else {
                                            Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                                            errorPlayer.start()
                                        }
                                    }
                            } else {
                                val exception = task.exception
                                if (exception is FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(this, "Invalid email or password format.", Toast.LENGTH_SHORT).show()
                                    errorPlayer.start()
                                } else {
                                    Toast.makeText(this, "Authentication failed: ${exception?.message}", Toast.LENGTH_SHORT).show()
                                    errorPlayer.start()
                                }
                            }
                        }
                } else {
                    Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                    errorPlayer.start()
                }
            } else {
                Toast.makeText(this, "Fill all required fields.", Toast.LENGTH_SHORT).show()
                errorPlayer.start()
            }
        }
    }
}
