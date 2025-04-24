package com.example.expencetracker.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.example.expencetracker.R
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.databinding.ActivityOnboardingBinding
import com.example.expencetracker.onboarding.CurrencyAdapter
import com.example.expencetracker.onboarding.OnboardingViewPagerAdapter
import com.example.expencetracker.utils.PrefUtil
import com.google.android.material.textfield.TextInputEditText

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardingViewPagerAdapter
    private lateinit var prefUtil: PrefUtil
    private var pin: String = ""
    private var selectedCurrency: String = "USD"
    private var currencyAdapter: CurrencyAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefUtil = PrefUtil(this)
        
        // Initialize PrefsManager
        PrefsManager.init(this)

        setupViewPager()
        setupButtons()
    }

    private fun setupViewPager() {
        adapter = OnboardingViewPagerAdapter(this)
        binding.viewPager.adapter = adapter
        
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                updateButtonVisibility(position)
                
                // Setup currency slide (slide 3) when it becomes visible
                if (position == 2) {
                    setupCurrencySlide()
                }
                
                // Setup PIN screen (slide 4) when it becomes visible
                if (position == 3) {
                    setupPinSlide()
                }
                
                // Setup finish screen (slide 5) when it becomes visible
                if (position == 4) {
                    setupFinishSlide()
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem < adapter.count - 1) {
                binding.viewPager.currentItem = binding.viewPager.currentItem + 1
            }
        }

        binding.btnSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun updateButtonVisibility(position: Int) {
        // Hide buttons on PIN setup and finish slides
        if (position == 3 || position == 4) {
            binding.btnNext.visibility = View.GONE
            binding.btnSkip.visibility = View.GONE
        } else {
            binding.btnNext.visibility = View.VISIBLE
            
            // Only show skip button before last slide
            binding.btnSkip.visibility = if (position < adapter.count - 2) View.VISIBLE else View.GONE
        }
    }

    private fun setupCurrencySlide() {
        val view = adapter.getSlideView(2)
        
        val rvCurrencies = view.findViewById<RecyclerView>(R.id.rvCurrencies)
        
        // Get current currency from preferences or use default
        selectedCurrency = PrefsManager.getCurrency()
        
        // Initialize adapter if not already done
        if (currencyAdapter == null) {
            currencyAdapter = CurrencyAdapter { currency ->
                selectedCurrency = currency
                PrefsManager.setCurrency(currency)
            }
        }
        
        // Set adapter and select the current currency
        rvCurrencies.adapter = currencyAdapter
        currencyAdapter?.setSelectedCurrency(selectedCurrency)
    }

    private fun setupPinSlide() {
        val view = adapter.getSlideView(3)
        
        val etPin = view.findViewById<TextInputEditText>(R.id.etPin)
        val etConfirmPin = view.findViewById<TextInputEditText>(R.id.etConfirmPin)
        val btnSkipPin = view.findViewById<Button>(R.id.btnSkipPin)
        
        // Setup PIN input listeners
        etPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 4) {
                    etConfirmPin.requestFocus()
                }
            }
        })
        
        etConfirmPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 4) {
                    validateAndSavePin(etPin.text.toString(), s.toString())
                }
            }
        })
        
        btnSkipPin.setOnClickListener {
            // Skip PIN setup and move to next slide
            binding.viewPager.currentItem = binding.viewPager.currentItem + 1
        }
    }
    
    private fun validateAndSavePin(pin: String, confirmPin: String) {
        if (pin == confirmPin) {
            // Save PIN using both the old PrefUtil and new PrefsManager
            this.pin = pin
            prefUtil.savePIN(pin)
            prefUtil.setAppLock(true)
            
            // Also save to PrefsManager for future use
            PrefsManager.setPin(pin)
            
            // Move to next slide
            binding.viewPager.currentItem = binding.viewPager.currentItem + 1
        } else {
            Toast.makeText(this, "PINs don't match. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupFinishSlide() {
        val view = adapter.getSlideView(4)
        
        val btnGetStarted = view.findViewById<Button>(R.id.btnGetStarted)
        
        btnGetStarted.setOnClickListener {
            finishOnboarding()
        }
    }
    
    private fun finishOnboarding() {
        // Mark onboarding as seen using both preference systems
        prefUtil.setFirstTimeLaunch(false)
        PrefsManager.setOnboardingSeen(true)
        
        // Start MainActivity with fully qualified class name
        startActivity(Intent(this, com.example.expencetracker.ui.MainActivity::class.java))
        finish()
    }
} 