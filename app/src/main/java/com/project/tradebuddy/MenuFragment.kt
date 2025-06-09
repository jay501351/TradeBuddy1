package com.project.tradebuddy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.project.tradebuddy.databinding.FragmentMenuBinding

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()

        loadUserDetails()
        setupMenuListeners()

        return binding.root
    }

    private fun loadUserDetails() {
        val sharedPref = requireContext().getSharedPreferences("UserDetails", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "User")
        binding.txtUserName.text = "Hello, $name"
    }

    private fun setupMenuListeners() {
        binding.menuProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.menuNotification.setOnClickListener {
            Toast.makeText(requireContext(), "Notification clicked", Toast.LENGTH_SHORT).show()
            // TODO: startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }

        binding.menuSettings.setOnClickListener {
            Toast.makeText(requireContext(), "Settings clicked", Toast.LENGTH_SHORT).show()
            // TODO: startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        binding.menuAbout.setOnClickListener {
            Toast.makeText(requireContext(), "About clicked", Toast.LENGTH_SHORT).show()
            // TODO: startActivity(Intent(requireContext(), AboutActivity::class.java))
        }

        binding.menuLogout.setOnClickListener {
            auth.signOut()
            val sharedPref = requireContext().getSharedPreferences("UserDetails", Context.MODE_PRIVATE)
            sharedPref.edit().clear().apply()

            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
