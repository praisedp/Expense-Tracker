package com.example.expencetracker.onboarding

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.example.expencetracker.R
import java.lang.ref.WeakReference

class OnboardingViewPagerAdapter(private val context: Context) : PagerAdapter() {
    
    // Keep a weak reference to the views to access them later (for PIN and Finish slides)
    private val slideViews = arrayOfNulls<WeakReference<View>>(5)
    
    // Slide layouts
    private val layouts = intArrayOf(
        R.layout.slide_welcome,     // Slide 1
        R.layout.slide_features,    // Slide 2
        R.layout.slide_currency,    // Slide 3
        R.layout.slide_pin,         // PIN setup slide
        R.layout.slide_finish       // Finish slide
    )
    
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(layouts[position], container, false)
        
        // Store a weak reference to the view for later access
        slideViews[position] = WeakReference(view)
        
        container.addView(view)
        return view
    }
    
    override fun getCount(): Int = layouts.size
    
    override fun isViewFromObject(view: View, obj: Any): Boolean = view === obj
    
    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        container.removeView(obj as View)
        slideViews[position] = null
    }
    
    /**
     * Get the slide view at the specified position
     * Used to access views for PIN setup and finish slides
     */
    fun getSlideView(position: Int): View {
        val weakRef = slideViews[position]
        return weakRef?.get() ?: throw IllegalStateException("View at position $position not found or already recycled")
    }
} 