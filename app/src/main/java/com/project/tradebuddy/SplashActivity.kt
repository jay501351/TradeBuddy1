package com.project.tradebuddy

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        // Start your animation
        val animation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.aplha)
        findViewById<View>(R.id.main).startAnimation(animation)

        auth = FirebaseAuth.getInstance()

        // Wait for 3 seconds before proceeding
        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = auth.currentUser
            if (currentUser != null && currentUser.isEmailVerified) {
                // User is signed in and verified
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                // Not signed in or email not verified
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish() // Close SplashActivity
        }, 3000)
    }
}
