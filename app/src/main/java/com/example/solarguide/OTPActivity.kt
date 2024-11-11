    package com.example.solarguide

    import android.content.Intent
    import android.os.Bundle
    import android.os.Handler
    import android.os.Looper
    import android.text.Editable
    import android.text.TextWatcher
    import android.util.Log
    import android.view.View
    import android.widget.*
    import androidx.appcompat.app.AppCompatActivity
    import com.google.firebase.FirebaseException
    import com.google.firebase.FirebaseTooManyRequestsException
    import com.google.firebase.auth.*
    import java.util.concurrent.TimeUnit

    class OTPActivity : AppCompatActivity() {

        private lateinit var auth: FirebaseAuth
        private lateinit var verifyBtn: Button
        private lateinit var resendTV: TextView
        private lateinit var inputOTP1: EditText
        private lateinit var inputOTP2: EditText
        private lateinit var inputOTP3: EditText
        private lateinit var inputOTP4: EditText
        private lateinit var inputOTP5: EditText
        private lateinit var inputOTP6: EditText
        private lateinit var progressBar: ProgressBar

        private lateinit var otp: String
        private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
        private lateinit var phoneNumber: String

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_otpactivity)

            otp = intent.getStringExtra("OTP").toString()
            resendToken = intent.getParcelableExtra("resendToken")!!
            phoneNumber = intent.getStringExtra("phoneNumber")!!

            init()
            progressBar.visibility = View.INVISIBLE
            addTextChangeListener()
            resendOTPTvVisibility()

            resendTV.setOnClickListener {
                resendVerificationCode()
                resendOTPTvVisibility()
            }

            verifyBtn.setOnClickListener {
                // Collect OTP from all the edit texts
                val typedOTP = (inputOTP1.text.toString() + inputOTP2.text.toString() + inputOTP3.text.toString()
                        + inputOTP4.text.toString() + inputOTP5.text.toString() + inputOTP6.text.toString())

                if (typedOTP.isNotEmpty()) {
                    if (typedOTP.length == 6) {
                        val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(
                            otp, typedOTP
                        )
                        progressBar.visibility = View.VISIBLE
                        signInWithPhoneAuthCredential(credential)
                    } else {
                        Toast.makeText(this, "Please Enter Correct OTP", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please Enter OTP", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun resendOTPTvVisibility() {
            inputOTP1.setText("")
            inputOTP2.setText("")
            inputOTP3.setText("")
            inputOTP4.setText("")
            inputOTP5.setText("")
            inputOTP6.setText("")
            resendTV.visibility = View.INVISIBLE
            resendTV.isEnabled = false

            Handler(Looper.myLooper()!!).postDelayed({
                resendTV.visibility = View.VISIBLE
                resendTV.isEnabled = true
            }, 60000)
        }

        private fun resendVerificationCode() {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(this)                 // Activity (for callback binding)
                .setCallbacks(callbacks)
                .setForceResendingToken(resendToken) // OnVerificationStateChangedCallbacks
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }

        private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                if (e is FirebaseAuthInvalidCredentialsException) {
                    Log.d("TAG", "onVerificationFailed: Invalid credentials - ${e.message}")
                } else if (e is FirebaseTooManyRequestsException) {
                    Log.d("TAG", "onVerificationFailed: SMS quota exceeded - ${e.message}")
                }
                progressBar.visibility = View.INVISIBLE
                Toast.makeText(this@OTPActivity, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                otp = verificationId
                resendToken = token
                Toast.makeText(this@OTPActivity, "OTP sent successfully.", Toast.LENGTH_SHORT).show()
            }
        }

        private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Authenticated Successfully", Toast.LENGTH_SHORT).show()
                        sendToMain()
                    } else {
                        Log.d("TAG", "signInWithPhoneAuthCredential: ${task.exception.toString()}")
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(this, "Invalid OTP entered.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    progressBar.visibility = View.INVISIBLE
                }
        }

        private fun sendToMain() {
            startActivity(Intent(this, AccountActivity::class.java))
        }

        private fun addTextChangeListener() {
            inputOTP1.addTextChangedListener(EditTextWatcher(inputOTP1))
            inputOTP2.addTextChangedListener(EditTextWatcher(inputOTP2))
            inputOTP3.addTextChangedListener(EditTextWatcher(inputOTP3))
            inputOTP4.addTextChangedListener(EditTextWatcher(inputOTP4))
            inputOTP5.addTextChangedListener(EditTextWatcher(inputOTP5))
            inputOTP6.addTextChangedListener(EditTextWatcher(inputOTP6))
        }

        private fun init() {
            auth = FirebaseAuth.getInstance()
            progressBar = findViewById(R.id.otpProgressBar)
            verifyBtn = findViewById(R.id.verifyOTPBtn)
            resendTV = findViewById(R.id.resendTextView)
            inputOTP1 = findViewById(R.id.otpEditText1)
            inputOTP2 = findViewById(R.id.otpEditText2)
            inputOTP3 = findViewById(R.id.otpEditText3)
            inputOTP4 = findViewById(R.id.otpEditText4)
            inputOTP5 = findViewById(R.id.otpEditText5)
            inputOTP6 = findViewById(R.id.otpEditText6)
        }

        inner class EditTextWatcher(private val view: View) : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                when (view.id) {
                    R.id.otpEditText1 -> if (text.length == 1) inputOTP2.requestFocus()
                    R.id.otpEditText2 -> if (text.length == 1) inputOTP3.requestFocus() else if (text.isEmpty()) inputOTP1.requestFocus()
                    R.id.otpEditText3 -> if (text.length == 1) inputOTP4.requestFocus() else if (text.isEmpty()) inputOTP2.requestFocus()
                    R.id.otpEditText4 -> if (text.length == 1) inputOTP5.requestFocus() else if (text.isEmpty()) inputOTP3.requestFocus()
                    R.id.otpEditText5 -> if (text.length == 1) inputOTP6.requestFocus() else if (text.isEmpty()) inputOTP4.requestFocus()
                    R.id.otpEditText6 -> if (text.isEmpty()) inputOTP5.requestFocus()
                }
            }
        }
    }
