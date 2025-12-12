package com.project.tradebuddy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.project.tradebuddy.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var loginBinding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loginBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(loginBinding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        loginBinding.btnLogin.setOnClickListener {
            loginUser()
        }

        loginBinding.txtSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        loginBinding.txtForgotPass.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun loginUser() {
        val email = loginBinding.edtEmail.text.toString().trim()
        val pass = loginBinding.edtPass.text.toString()

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Email or Password must not be empty", Toast.LENGTH_SHORT).show()
            return
        }

        loginBinding.btnLogin.isEnabled = false
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this) { task ->
            loginBinding.btnLogin.isEnabled = true
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user != null) {
                    if (user.isEmailVerified) {
                        // Set Firestore user in WatchlistManager (if implemented)
                        WatchlistManager.setFirestoreUser(user.uid)

                        // --- DEBUG WRITE: attempt a test write to Firestore and log the result ---
                        performFirestoreDebugWrite(user.uid)

                        // Attempt to sync watchlist (your existing flow). Continue to Home regardless of sync result.
                        WatchlistManager.syncWithFirestore(this) { success, msg ->
                            if (success) {
                                Toast.makeText(this, "Watchlist synced", Toast.LENGTH_SHORT).show()
                            } else {
                                if (!msg.isNullOrEmpty()) {
                                    Toast.makeText(this, "Watchlist sync failed: $msg", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Watchlist sync failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                            // Continue to fetch user profile / open home regardless of sync success
                            fetchUserDataAndProceed(user.uid)
                        }
                    } else {
                        Toast.makeText(this, "Please verify your email", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                    }
                } else {
                    Toast.makeText(this, "Login succeeded but user is null", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Perform a one-off debug write to Firestore under:
     *   users/{uid}/watchlists/___debug_test
     *
     * Shows a toast and writes a Log entry with success/failure so you can confirm in Logcat.
     */
    private fun performFirestoreDebugWrite(uid: String) {
        try {
            val testDocRef = firestore.collection("users")
                .document(uid)
                .collection("watchlists")
                .document("___debug_test")

            val sampleStock = mapOf(
                "symbol" to "AAPL",
                "instrument_name" to "Apple Inc.",
                "exchange" to "NASDAQ",
                "country" to "US",
                "currency" to "USD"
            )

            val payload = mapOf("stocks" to listOf(sampleStock), "debug_ts" to System.currentTimeMillis())

            testDocRef.set(payload, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Firestore debug write succeeded for uid=$uid")
                    Toast.makeText(this, "Debug write succeeded (check Firestore console)", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firestore debug write failed for uid=$uid: ${e.message}", e)
                    Toast.makeText(this, "Debug write failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while attempting debug write: ${e.message}", e)
            Toast.makeText(this, "Debug write failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Fetch user profile fields from Firestore (users/{uid}) and then open HomeActivity.
     * If fetch fails, still proceed but show a toast.
     */
    private fun fetchUserDataAndProceed(userId: String) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: ""
                    val email = document.getString("email") ?: ""

                    // Save user data locally
                    val sharedPref = getSharedPreferences("UserDetails", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("name", name)
                        putString("email", email)
                        apply()
                    }

                    Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                } else {
                    // No profile doc found — still continue
                    Toast.makeText(this, "User data not found, continuing...", Toast.LENGTH_SHORT).show()
                }

                // Open home
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
            .addOnFailureListener { ex ->
                // Log + continue
                Log.w(TAG, "Failed to fetch user profile doc: ${ex.message}", ex)
                Toast.makeText(this, "Failed to fetch user data: ${ex.message}", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
    }
}
