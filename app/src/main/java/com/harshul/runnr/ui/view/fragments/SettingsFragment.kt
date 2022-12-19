package com.harshul.runnr.ui.view.fragments

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.harshul.runnr.R
import com.harshul.runnr.databinding.FragmentSettingsBinding
import com.harshul.runnr.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import java.text.MessageFormat
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadFieldsFromSharedPref()
        binding.tvVersion.text = MessageFormat.format("V:{0}", getVersionCode())

        binding.btnApplyChanges.setOnClickListener {
            val success = applyChangesToSharedPref()
            if (success) Snackbar.make(view, "Changes saved", Snackbar.LENGTH_SHORT).show()
            else Snackbar.make(view, "Please enter all fields", Snackbar.LENGTH_SHORT).show()
        }

    }

    private fun getVersionCode(): String {
        try {
            val pInfo = requireContext().packageManager.getPackageInfo(
                requireContext().packageName, 0
            )
            return pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return "1.0.0"
    }

    private fun loadFieldsFromSharedPref() {
        val name = sharedPreferences.getString(Constants.KEY_NAME, "")
        val weight = sharedPreferences.getFloat(Constants.KEY_WEIGHT, 80f)
        binding.etName.setText(name)
        binding.etWeight.setText(weight.toString())

    }

    private fun applyChangesToSharedPref(): Boolean {
        val nameText = binding.etName.text.toString().trim()
        val weightText = binding.etWeight.text.toString().trim()
        if (nameText.isEmpty() || weightText.isEmpty()) {
            return false
        }
        sharedPreferences.edit()
            .putString(Constants.KEY_NAME, nameText)
            .putFloat(Constants.KEY_WEIGHT, weightText.toFloat())
            .apply()
        return true
    }

}