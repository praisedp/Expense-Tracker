package com.example.expencetracker.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import android.util.Log

object CurrencyConverter {

    /**
     * Fetches the conversion rate from [base] currency to [target] currency.
     * Returns 1.0 as a fallback if the API call fails or if the expected fields are missing.
     */
    suspend fun fetchConversionRate(base: String, target: String): Double {
        return withContext(Dispatchers.IO) {
            try {
                // Your API key - remember, this is visible in code for now.
                val apiKey = "60ba907ea2685b77247441a6"
                // The API endpoint for exchangerate-api.com (v6). Replace 'latest' path if needed per API documentation.
                val url = "https://v6.exchangerate-api.com/v6/$apiKey/latest/$base"
                val responseText = URL(url).readText()
                Log.d("CurrencyDebug", "Raw JSON response: $responseText")

                val jsonObject = JSONObject(responseText)
                // Check for success status.
                if (jsonObject.getString("result") != "success") {
                    Log.d("CurrencyDebug", "API call failed: ${jsonObject.optString("error-type")}")
                    return@withContext 1.0
                }
                // Get the conversion rates object.
                val ratesObject = jsonObject.getJSONObject("conversion_rates")
                if (!ratesObject.has(target)) {
                    Log.d("CurrencyDebug", "JSON 'conversion_rates' does not contain key for $target.")
                    return@withContext 1.0
                }
                val rate = ratesObject.getDouble(target)
                Log.d("CurrencyDebug", "Fetched rate $rate for converting from $base to $target.")
                rate
            } catch (e: Exception) {
                e.printStackTrace()
                1.0
            }
        }
    }
}