package com.project.tradebuddy

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class ProfileFragment : Fragment() {

    private lateinit var txtName: TextView
    private lateinit var txtEmail: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        txtName = view.findViewById<TextView>(R.id.txtProfileName)
        txtEmail = view.findViewById<TextView>(R.id.txtProfileEmail)

        //Fetch data from shared preferences
        val sharedPref = requireContext().getSharedPreferences("UserDetails", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "User")
        val email = sharedPref.getString("email", "example@gmail.com")

        txtName.text = "Name: $name"
        txtEmail.text= "Email: $email"
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

}