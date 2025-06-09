package com.project.tradebuddy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tradebuddy.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {
    private lateinit var signupBinding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db : FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        signupBinding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(signupBinding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val sharedPref = getSharedPreferences("user_prefs",Context.MODE_PRIVATE)

        signupBinding.btnSignup.setOnClickListener {
            val name = signupBinding.edtSignupName.text.toString()
            val email = signupBinding.edtSignupEmail.text.toString()
            val pass = signupBinding.edtSignupPass.text.toString()
            val conPass = signupBinding.edtSignupConPass.text.toString()

            if(name.isEmpty() || email.isEmpty() || pass.isEmpty() || conPass.isEmpty()){
                Toast.makeText(this, "All Fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(pass != conPass){
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid

                    val userMap = hashMapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email
                    )

                    uid?.let {
                        db.collection("users").document(it)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "user registered online", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }

                    sharedPref.edit().apply{
                        putString("user_uid",uid)
                        putString("user_name",name)
                        putString("user_email",email)
                            .apply()
                    }

                    user?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                        if (verifyTask.isSuccessful) {
                            Toast.makeText(this, "Verification email sent", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }else{
                            Toast.makeText(this, "Failed to send verification email", Toast.LENGTH_SHORT).show()
                        }
                    }
                }else{
                    Toast.makeText(this, "Error:${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        signupBinding.txtLoginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}