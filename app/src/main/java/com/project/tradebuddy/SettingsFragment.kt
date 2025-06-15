package com.project.tradebuddy

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.project.tradebuddy.databinding.FragmentSettingsBinding
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate


class SettingsFragment : Fragment() {
    private lateinit var settingsBinding: FragmentSettingsBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        settingsBinding = FragmentSettingsBinding.bind(view)

        val sharedPref = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode",false)

        settingsBinding.switchTheme.isChecked = isDarkMode

        settingsBinding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPref.edit()
            editor.putBoolean("dark_mode", isChecked)
            editor.apply()

            AppCompatDelegate.setDefaultNightMode(
                if(isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        return view
    }
}