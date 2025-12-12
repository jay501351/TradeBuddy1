package com.project.tradebuddy

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tradebuddy.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private lateinit var profileBinding: FragmentProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        profileBinding = FragmentProfileBinding.bind(view)

        // Load and show saved profile values
        loadProfileFromPrefs()

        // Edit icon click -> open update dialog
        profileBinding.btnEditProfile.setOnClickListener {
            showUpdateProfileDialog()
        }

        return view
    }

    private fun loadProfileFromPrefs() {
        val sharedPref = requireContext().getSharedPreferences("UserDetails", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "User")
        val email = sharedPref.getString("email", "example@gmail.com")

        Log.d("ProfileFragment", "Loaded profile from prefs -> name: $name, email: $email")
        profileBinding.txtProfileName.text = name
        profileBinding.txtProfileEmail.text = email
    }

    /**
     * Shows a dialog allowing the user to update their display name.
     * On save, updates:
     *  - Firestore users/{uid}.name
     *  - FirebaseAuth currentUser.displayName
     *  - SharedPreferences
     *  - UI
     */
    private fun showUpdateProfileDialog() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in. Please login to update profile.", Toast.LENGTH_SHORT).show()
            return
        }

        // prefill with current name (from UI / prefs)
        val currentName = profileBinding.txtProfileName.text?.toString() ?: ""
        val input = EditText(requireContext()).apply {
            setText(currentName)
            hint = "Full name"
            setSelection(text.length)
            setSingleLine()
            // small padding for nicer look
            setPadding(20, 20, 20, 20)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Update Profile")
            .setMessage("Update your display name")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@setPositiveButton
                }

                // disable dialog while saving (dialog will dismiss automatically on listeners)
                updateProfileName(user.uid, user.uid, newName) // pass uid twice; first param unused now but kept simple
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun updateProfileName(userIdForFirestore: String, firebaseUid: String, newName: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // 1) Update Firestore users/{uid}.name
        val userDocRef = firestore.collection("users").document(user.uid)
        val updates = mapOf("name" to newName)

        userDocRef
            .update(updates)
            .addOnSuccessListener {
                Log.d("ProfileFragment", "Firestore name updated")
                // 2) Update FirebaseAuth displayName
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()

                user.updateProfile(profileUpdates)
                    .addOnSuccessListener {
                        Log.d("ProfileFragment", "FirebaseAuth displayName updated")
                        // 3) Save locally to SharedPreferences
                        val sharedPref = requireContext().getSharedPreferences("UserDetails", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("name", newName)
                            apply()
                        }
                        // 4) Update UI
                        profileBinding.txtProfileName.text = newName
                        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { ex ->
                        Log.w("ProfileFragment", "Failed updating FirebaseAuth profile: ${ex.message}")
                        // still save name to prefs and show message that Firestore updated but auth failed
                        val sharedPref = requireContext().getSharedPreferences("UserDetails", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("name", newName)
                            apply()
                        }
                        profileBinding.txtProfileName.text = newName
                        Toast.makeText(requireContext(), "Saved name to Firestore but failed to update auth profile", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { ex ->
                // If document doesn't exist — create it (set)
                Log.w("ProfileFragment", "Firestore update failed: ${ex.message}. Trying set() as fallback.")
                // create basic doc
                userDocRef
                    .set(mapOf("name" to newName, "email" to (user.email ?: "")))
                    .addOnSuccessListener {
                        Log.d("ProfileFragment", "Firestore user doc created with name")
                        // now update FirebaseAuth displayName
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(newName)
                            .build()
                        user.updateProfile(profileUpdates)
                            .addOnSuccessListener {
                                val sharedPref = requireContext().getSharedPreferences("UserDetails", Context.MODE_PRIVATE)
                                with(sharedPref.edit()) {
                                    putString("name", newName)
                                    apply()
                                }
                                profileBinding.txtProfileName.text = newName
                                Toast.makeText(requireContext(), "Profile created & updated", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { ex2 ->
                                Log.w("ProfileFragment", "Failed updating FirebaseAuth profile after set(): ${ex2.message}")
                                val sharedPref = requireContext().getSharedPreferences("UserDetails", Context.MODE_PRIVATE)
                                with(sharedPref.edit()) {
                                    putString("name", newName)
                                    apply()
                                }
                                profileBinding.txtProfileName.text = newName
                                Toast.makeText(requireContext(), "Saved name to Firestore but failed to update auth profile", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { ex2 ->
                        Log.e("ProfileFragment", "Failed creating Firestore user doc: ${ex2.message}")
                        Toast.makeText(requireContext(), "Failed to save profile: ${ex2.message}", Toast.LENGTH_LONG).show()
                    }
            }
    }
}
