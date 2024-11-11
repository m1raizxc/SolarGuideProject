package com.example.solarguide

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog
import android.media.MediaPlayer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AccountActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var popPlayer: MediaPlayer
    private lateinit var buttonClickPlayer: MediaPlayer
    private lateinit var errorPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        popPlayer = MediaPlayer.create(this, R.raw.pop_sound)
        buttonClickPlayer = MediaPlayer.create(this, R.raw.button_click)
        errorPlayer = MediaPlayer.create(this, R.raw.error_sound)

        val frameLayoutIds = listOf(
            R.id.WeatherFrameButton,
            R.id.BatteryFrameButton,
            R.id.PanelFrameButton,
            R.id.DevicesFrameButton,
            R.id.AccountFrameButton
        )

        val activities = listOf(
            WeatherActivity::class.java,
            BatteryPerformanceActivity::class.java,
            PanelPerformanceActivity::class.java,
            AvailableDevicesActivity::class.java,
            AccountActivity::class.java
        )

        ButtonUtils.setMenuButtonClickListener(this, frameLayoutIds, activities)
        popPlayer.start()

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!

        // Reset Password Button
        val resetPasswordBtn: Button = findViewById(R.id.resetPasswordBtn)
        resetPasswordBtn.setOnClickListener {
            resetPassword(currentUser.email!!)
            buttonClickPlayer.start()
        }

        // Logout Button
        val logoutButton: Button = findViewById(R.id.btn_logout)
        logoutButton.setOnClickListener {
            logout()
            buttonClickPlayer.start()
        }

        // Delete Account Button
        val deleteAccountBtn: Button = findViewById(R.id.deleteAccountBtn)
        deleteAccountBtn.setOnClickListener {
            confirmDeleteAccount()
            buttonClickPlayer.start()
        }

        // FAQ Button
        val faqsBtn: Button = findViewById(R.id.FAQsBtn)
        faqsBtn.setOnClickListener {
            showFaqDialog()
            buttonClickPlayer.start()
        }

        // Support Button
        val supportBtn: Button = findViewById(R.id.SupportBtn)
        supportBtn.setOnClickListener {
            showSupportDialog()
            buttonClickPlayer.start()
        }
    }

    private fun resetPassword(email: String) {
        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("A password reset email will be sent to $email. Do you want to proceed?")
            .setPositiveButton("Yes") { _, _ ->
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_SHORT).show()
                            // Redirect to login page
                            val intent = Intent(this, SignInActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }


    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        buttonClickPlayer.start()
        finish()
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to permanently delete your account?")
            .setPositiveButton("Yes") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun deleteAccount() {
        currentUser.delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                    // Redirect to landing page
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to delete account: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    errorPlayer.start()
                }
            }
    }


    private fun showFaqDialog() {
        AlertDialog.Builder(this)
            .setTitle("Frequently Asked Questions")
            .setMessage(
                """
                1. General Information
What is SolarGuide?
SolarGuide is a comprehensive solar energy monitoring app that helps you track weather conditions, battery performance, panel efficiency, and manage connected devices.

2. Account Management
How do I create an account?
To create an account, download the SolarGuide app, open it, and select 'Sign Up.' You can register using your email and password.

How do I reset my password?
Go to the login page, click 'Forgot Password,' and enter your registered email address. Follow the instructions sent to your email to reset your password.

How do I verify my email address?
After signing up, you'll receive an email with a verification link. Click the link to verify your email address.

Can I login using my phone number to my account?
Yes, you can login using your phone number to the login page and verify the OTP sent to your number to successfully access SolarGuide features.

3. Using the App
How do I monitor my solar panel performance?
Navigate to the 'Panel Performance' section in the app to view detailed statistics about your solar panel efficiency and output.

How can I track battery performance?
Go to the 'Battery Performance' section to monitor the health and charge levels of your solar battery.

How do I check weather conditions?
The 'Weather Monitoring' feature provides real-time updates and forecasts relevant to your solar setup.

How do I view available devices?
Visit the 'Device Management' section to monitor the available devices for energy stored in your battery.

4. Troubleshooting
I’m not receiving the email verification link. What should I do?
Check your spam/junk folder. Ensure that you entered the correct email address. If the issue persists, contact support.

I forgot my password and can’t access my email. What should I do?
Contact support for assistance with account recovery.

The app is not syncing data from my solar devices. What should I do?
Ensure your devices are properly connected and have internet access. If the problem continues, restart the app or your device, and check for updates.
                """.trimIndent()
            )
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showSupportDialog() {
        AlertDialog.Builder(this)
            .setTitle("Customer Support & Feedback")
            .setMessage(
                """
                1. Contact Us
Email Support:
For any issues or inquiries, email us at SolarGuideSupp2024@gmail.com.

Phone Support:
Call our support line at +63-918-3954-489 during business hours.

2. Community Forum
Join Our Community:
Visit our community forum to ask questions, share tips, and connect with other SolarGuide users.

3. Documentation and Tutorials
User Manual:
Access our user manual for detailed instructions on using the app.


4. Feedback and Suggestions
We Value Your Feedback:
Send your feedback and suggestions to SolarGuideSupp2024@gmail.com. We’re always looking to improve our app and services.

By providing clear and concise information in these sections, you can enhance user experience and reduce the volume of support inquiries.
                """.trimIndent()
            )
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}
