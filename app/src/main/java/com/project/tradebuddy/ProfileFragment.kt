package com.project.tradebuddy

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.project.tradebuddy.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private lateinit var profileBinding: FragmentProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        profileBinding = FragmentProfileBinding.bind(view)

        //Fetch data from shared preferences
        val sharedPref = requireContext().getSharedPreferences("UserDetails", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "User")
        val email = sharedPref.getString("email", "example@gmail.com")

        Log.d("ProfileFragment", "Name: $name, Email: $email")


        profileBinding.txtProfileName.text = "$name"
        profileBinding.txtProfileEmail.text= "$email"
        return view
    }

}