package com.example.expencetracker.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.expencetracker.R
import com.example.expencetracker.data.PrefsManager
import com.example.expencetracker.utils.PrefUtil

class SplashActivity : AppCompatActivity() {

    private val splashDuration = 3000L // 3 seconds
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Initialize PrefsManager
        PrefsManager.init(this)
        
        // Initialize views
        val logoContainer = findViewById<ConstraintLayout>(R.id.logoContainer)
        val appName = findViewById<TextView>(R.id.tvAppName)
        val tagline = findViewById<TextView>(R.id.tvTagline)
        val logo = findViewById<ImageView>(R.id.ivLogo)
        
        // Start animations
        startLogoAnimation(logoContainer, logo)
        startTextAnimations(appName, tagline)
        
        // Navigate after splash duration
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, splashDuration)
    }
    
    private fun navigateToNextScreen() {
        // Check for backwards compatibility with old preference system
        val prefUtil = PrefUtil(this)
        val shouldShowOnboarding = PrefsManager.shouldShowOnboarding() || prefUtil.isFirstTimeLaunch()
        
        val intent = if (shouldShowOnboarding) {
            // First time launch or forced onboarding, go to OnboardingActivity
            Intent(this, com.example.expencetracker.ui.OnboardingActivity::class.java)
        } else {
            // Normal launch, go to MainActivity
            Intent(this, com.example.expencetracker.ui.MainActivity::class.java)
        }
        
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
    
    private fun startLogoAnimation(logoContainer: View, logo: ImageView) {
        // Fade in animation for the container
        val fadeIn = ObjectAnimator.ofFloat(logoContainer, "alpha", 0f, 1f)
        fadeIn.duration = 800
        fadeIn.interpolator = DecelerateInterpolator()
        
        // Scale animation for the logo
        val scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.2f, 1f)
        
        scaleX.duration = 1000
        scaleY.duration = 1000
        
        // Add a slight bounce effect
        scaleX.interpolator = AnticipateOvershootInterpolator(1.0f)
        scaleY.interpolator = AnticipateOvershootInterpolator(1.0f)
        
        // Rotation animation
        val rotation = ObjectAnimator.ofFloat(logo, "rotation", 0f, 360f)
        rotation.duration = 1200
        rotation.interpolator = AccelerateDecelerateInterpolator()
        
        // Play animations together
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fadeIn, scaleX, scaleY, rotation)
        animatorSet.start()
    }
    
    private fun startTextAnimations(appName: TextView, tagline: TextView) {
        // Fade in and slide up for app name
        val appNameFadeIn = ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f)
        val appNameSlideUp = ObjectAnimator.ofFloat(appName, "translationY", 50f, 0f)
        
        appNameFadeIn.duration = 800
        appNameSlideUp.duration = 800
        appNameFadeIn.startDelay = 500
        appNameSlideUp.startDelay = 500
        
        // Fade in and slide up for tagline
        val taglineFadeIn = ObjectAnimator.ofFloat(tagline, "alpha", 0f, 1f)
        val taglineSlideUp = ObjectAnimator.ofFloat(tagline, "translationY", 30f, 0f)
        
        taglineFadeIn.duration = 800
        taglineSlideUp.duration = 800
        taglineFadeIn.startDelay = 700
        taglineSlideUp.startDelay = 700
        
        // Play animations together
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            appNameFadeIn, appNameSlideUp,
            taglineFadeIn, taglineSlideUp
        )
        animatorSet.start()
    }
} 