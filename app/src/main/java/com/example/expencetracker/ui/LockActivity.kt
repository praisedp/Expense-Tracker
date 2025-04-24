package com.example.expencetracker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.expencetracker.R
import com.example.expencetracker.data.PrefsManager

/**
 * PIN entry screen that appears when the app is locked
 */
class LockActivity : AppCompatActivity() {

    private lateinit var etPin: EditText
    private lateinit var tvError: TextView
    private lateinit var btnUnlock: Button
    private lateinit var rootLayout: ConstraintLayout
    
    // PIN digit views
    private lateinit var pinDigits: List<TextView>
    
    private var attemptCount = 0
    private val MAX_ATTEMPTS = 5
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)
        
        rootLayout = findViewById(R.id.rootLayout)
        etPin = findViewById(R.id.etPin)
        tvError = findViewById(R.id.tvError)
        btnUnlock = findViewById(R.id.btnUnlock)
        
        // Initialize PIN digit views
        pinDigits = listOf(
            findViewById<TextView>(R.id.pinDigit1),
            findViewById<TextView>(R.id.pinDigit2),
            findViewById<TextView>(R.id.pinDigit3),
            findViewById<TextView>(R.id.pinDigit4)
        )
        
        setupListeners()
        
        // Set focus to the hidden PIN field to show keyboard
        etPin.requestFocus()
        showKeyboard()
        
        // Make tapping anywhere on the screen focus on PIN input
        rootLayout.setOnClickListener {
            focusOnPinInput()
        }
    }
    
    private fun setupListeners() {
        // Auto-submit when 4 digits are entered
        etPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updatePinDisplay(s.toString())
                
                if (s?.length == 4) {
                    verifyPin()
                }
                // Clear error when user starts typing again
                if (!s.isNullOrEmpty()) {
                    tvError.text = ""
                }
            }
        })
        
        btnUnlock.setOnClickListener {
            verifyPin()
        }
        
        // Make each PIN digit box clickable to focus on input
        pinDigits.forEach { digitView ->
            digitView.setOnClickListener {
                focusOnPinInput()
            }
        }
        
        // Also make the container clickable
        findViewById<View>(R.id.pinContainer).setOnClickListener {
            focusOnPinInput()
        }
    }
    
    private fun focusOnPinInput() {
        etPin.requestFocus()
        showKeyboard()
    }
    
    private fun showKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etPin, InputMethodManager.SHOW_IMPLICIT)
    }
    
    private fun updatePinDisplay(pin: String) {
        // Update each PIN digit indicator
        for (i in 0 until 4) {
            if (i < pin.length) {
                // Show a dot for entered digits
                pinDigits[i].text = "•"
            } else {
                // Clear display for unfilled positions
                pinDigits[i].text = ""
            }
        }
        
        // Enable/disable unlock button based on PIN length
        btnUnlock.isEnabled = pin.length == 4
    }
    
    private fun verifyPin() {
        val pin = etPin.text.toString()
        
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            showError("PIN must be 4 digits")
            return
        }
        
        if (PrefsManager.verifyPin(pin)) {
            // PIN is correct
            PrefsManager.markSessionUnlocked()
            setResult(RESULT_OK)
            finish()
        } else {
            // PIN is incorrect
            attemptCount++
            
            // Shake animation for wrong PIN
            val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
            findViewById<android.view.View>(R.id.pinContainer).startAnimation(shake)
            
            if (attemptCount >= MAX_ATTEMPTS) {
                showError("Too many attempts. Try again later.")
                btnUnlock.isEnabled = false
                // In a real app, you might implement a timeout here
            } else {
                val attemptsLeft = MAX_ATTEMPTS - attemptCount
                showError("Incorrect PIN. $attemptsLeft attempts left.")
            }
            
            etPin.text.clear()
            updatePinDisplay("")
        }
    }
    
    private fun showError(message: String) {
        tvError.text = message
        tvError.isVisible = message.isNotEmpty()
    }
    
    // Prevent back button from bypassing the lock
    override fun onBackPressed() {
        if (isTaskRoot) {
            // If this is the root activity, exit the app
            finishAffinity()
        } else {
            super.onBackPressed()
        }
    }
    
    companion object {
        const val REQUEST_CODE_UNLOCK = 1001
        
        fun start(context: Context) {
            val intent = Intent(context, LockActivity::class.java)
            context.startActivity(intent)
        }
        
        fun startForResult(activity: AppCompatActivity) {
            val intent = Intent(activity, LockActivity::class.java)
            activity.startActivityForResult(intent, REQUEST_CODE_UNLOCK)
        }
    }
} 