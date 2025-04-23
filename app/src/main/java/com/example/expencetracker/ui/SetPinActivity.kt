package com.example.expencetracker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.expencetracker.R
import com.example.expencetracker.data.PrefsManager

/**
 * Activity for setting up or changing the PIN
 */
class SetPinActivity : AppCompatActivity() {

    private lateinit var etNewPin: EditText
    private lateinit var etConfirmPin: EditText
    private lateinit var tvError: TextView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    
    private var mode = MODE_NEW_PIN
    private var currentPin: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_pin)
        
        // Get mode from intent
        mode = intent.getIntExtra(EXTRA_MODE, MODE_NEW_PIN)
        
        // In change mode, we need the current PIN for verification
        if (mode == MODE_CHANGE_PIN) {
            currentPin = intent.getStringExtra(EXTRA_CURRENT_PIN)
            if (currentPin.isNullOrEmpty()) {
                // Cannot change PIN without current PIN
                setResult(RESULT_CANCELED)
                finish()
                return
            }
        }
        
        // Initialize views
        etNewPin = findViewById(R.id.etNewPin)
        etConfirmPin = findViewById(R.id.etConfirmPin)
        tvError = findViewById(R.id.tvError)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        
        setupListeners()
    }
    
    private fun setupListeners() {
        // Add text watchers for PIN fields
        val pinWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Clear error when user starts typing
                tvError.text = ""
                
                // Auto-advance to confirmation field when 4 digits entered
                if (s != null && s === etNewPin.text && s.length == 4) {
                    etConfirmPin.requestFocus()
                }
                
                // Enable save button only when both fields have 4 digits
                btnSave.isEnabled = etNewPin.text?.length == 4 && etConfirmPin.text?.length == 4
            }
        }
        
        etNewPin.addTextChangedListener(pinWatcher)
        etConfirmPin.addTextChangedListener(pinWatcher)
        
        // Button listeners
        btnSave.setOnClickListener { savePin() }
        btnCancel.setOnClickListener { 
            setResult(RESULT_CANCELED)
            finish() 
        }
    }
    
    private fun savePin() {
        val newPin = etNewPin.text.toString()
        val confirmPin = etConfirmPin.text.toString()
        
        // Validate PIN format
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            showError("PIN must be 4 digits")
            return
        }
        
        // Check if PINs match
        if (newPin != confirmPin) {
            showError("PINs don't match")
            etConfirmPin.text.clear()
            etConfirmPin.requestFocus()
            return
        }
        
        // In change mode, verify the current PIN first
        if (mode == MODE_CHANGE_PIN && !PrefsManager.verifyPin(currentPin!!)) {
            showError("Current PIN is incorrect")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        
        // Save the new PIN
        if (PrefsManager.setPin(newPin)) {
            setResult(RESULT_OK)
            finish()
        } else {
            showError("Failed to set PIN")
        }
    }
    
    private fun showError(message: String) {
        tvError.text = message
    }
    
    companion object {
        const val REQUEST_CODE_SET_PIN = 1002
        const val REQUEST_CODE_CHANGE_PIN = 1003
        
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_CURRENT_PIN = "current_pin"
        
        const val MODE_NEW_PIN = 0
        const val MODE_CHANGE_PIN = 1
        
        /**
         * Start activity to set a new PIN
         */
        fun startForNewPin(activity: AppCompatActivity) {
            val intent = Intent(activity, SetPinActivity::class.java)
            intent.putExtra(EXTRA_MODE, MODE_NEW_PIN)
            activity.startActivityForResult(intent, REQUEST_CODE_SET_PIN)
        }
        
        /**
         * Start activity to change an existing PIN
         */
        fun startForChangePin(activity: AppCompatActivity, currentPin: String) {
            val intent = Intent(activity, SetPinActivity::class.java)
            intent.putExtra(EXTRA_MODE, MODE_CHANGE_PIN)
            intent.putExtra(EXTRA_CURRENT_PIN, currentPin)
            activity.startActivityForResult(intent, REQUEST_CODE_CHANGE_PIN)
        }
    }
} 