package com.chaitany.carbonview.AISuggestions

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chaitany.carbonview.R
import com.chaitany.carbonview.databinding.ActivityGetOnlyAiResponseBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GetOnlyAiResponse : AppCompatActivity() {

    private lateinit var binding: ActivityGetOnlyAiResponseBinding
    private lateinit var loadingDialog: androidx.appcompat.app.AlertDialog
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    private val apiKey: String by lazy { getString(R.string.gemini_api_key) }
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGetOnlyAiResponseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get the prompt from the intent
        val prompt = intent.getStringExtra("prompt")
        if (prompt.isNullOrEmpty()) {
            Toast.makeText(this, "No prompt provided", Toast.LENGTH_LONG).show()
            binding.tvAiResponse.text = "Error: No prompt provided."
            binding.tvAiResponse.visibility = View.VISIBLE
            animateResponseEntry()
            return
        }

        Log.d("GetOnlyAiResponse", "Received prompt: $prompt")

        // Show loading dialog and fetch AI response
        showLoadingDialog()
        fetchAiResponse(prompt)
    }

    private fun showLoadingDialog() {
        loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Fetching AI Insights")
            .setMessage("Generating emission reduction suggestions...")
            .setCancelable(false)
            .setView(layoutInflater.inflate(R.layout.dialog_progress, null)) // Custom layout with ProgressBar
            .create()
        loadingDialog.show()
    }

    private fun hideLoadingDialog() {
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    private fun fetchAiResponse(prompt: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val responseText = sendRequestToGemini(prompt)
            withContext(Dispatchers.Main) {
                hideLoadingDialog()
                binding.tvAiResponse.visibility = View.VISIBLE
                if (responseText != null) {
                    displayResponseWithTypewriterEffect(responseText)
                } else {
                    binding.tvAiResponse.text = "Failed to get AI response. Check logs for details."
                    Toast.makeText(this@GetOnlyAiResponse, "Failed to fetch AI response", Toast.LENGTH_LONG).show()
                    animateResponseEntry()
                }
            }
        }
    }

    private fun displayResponseWithTypewriterEffect(response: String) {
        val lines = response.split("\n").filter { it.isNotBlank() }
        var currentLineIndex = 0
        val spannableBuilder = SpannableStringBuilder()

        fun appendNextLine() {
            if (currentLineIndex >= lines.size) {
                return
            }

            val line = lines[currentLineIndex].trim()
            if (spannableBuilder.isNotEmpty()) {
                spannableBuilder.append("\n\n")
            }

            val formattedLine = formatMarkdownLine(line)
            spannableBuilder.append(formattedLine)

            binding.tvAiResponse.text = spannableBuilder
            binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN) }

            if (currentLineIndex == 0) {
                animateResponseEntry() // Animate only the first line entry
            }

            currentLineIndex++
            handler.postDelayed({ appendNextLine() }, 400) // Faster delay (400ms) for smoother effect
        }

        appendNextLine()
    }

    private fun animateResponseEntry() {
        binding.tvAiResponse.apply {
            alpha = 0f
            translationY = 50f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun formatMarkdownLine(line: String): CharSequence {
        val spannable = SpannableStringBuilder()
        var currentIndex = 0

        // Handle headers (## or #)
        if (line.startsWith("##")) {
            spannable.append(line.substring(2).trim(), StyleSpan(android.graphics.Typeface.BOLD), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            return spannable
        } else if (line.startsWith("#")) {
            spannable.append(line.substring(1).trim(), StyleSpan(android.graphics.Typeface.BOLD), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            return spannable
        }

        // Handle bold (**text** or *text*)
        while (currentIndex < line.length) {
            val nextBoldStart = line.indexOf("**", currentIndex)
            if (nextBoldStart == -1) {
                spannable.append(line.substring(currentIndex))
                break
            }

            spannable.append(line.substring(currentIndex, nextBoldStart))
            currentIndex = nextBoldStart + 2

            if (currentIndex >= line.length) break

            val nextBoldEnd = line.indexOf("**", currentIndex)
            if (nextBoldEnd == -1) {
                spannable.append(line.substring(nextBoldStart))
                break
            }

            val boldText = line.substring(currentIndex, nextBoldEnd)
            if (boldText.isNotEmpty()) {
                spannable.append(boldText, StyleSpan(android.graphics.Typeface.BOLD), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            currentIndex = nextBoldEnd + 2
        }

        // Fallback for single * bold
        if (line.contains("*") && !line.contains("**")) {
            spannable.clear()
            currentIndex = 0
            while (currentIndex < line.length) {
                val nextBoldStart = line.indexOf("*", currentIndex)
                if (nextBoldStart == -1) {
                    spannable.append(line.substring(currentIndex))
                    break
                }

                spannable.append(line.substring(currentIndex, nextBoldStart))
                currentIndex = nextBoldStart + 1

                if (currentIndex >= line.length) break

                val nextBoldEnd = line.indexOf("*", currentIndex)
                if (nextBoldEnd == -1) {
                    spannable.append(line.substring(nextBoldStart))
                    break
                }

                val boldText = line.substring(currentIndex, nextBoldEnd)
                if (boldText.isNotEmpty()) {
                    spannable.append(boldText, StyleSpan(android.graphics.Typeface.BOLD), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                currentIndex = nextBoldEnd + 1
            }
        }

        return spannable
    }

    private suspend fun sendRequestToGemini(prompt: String): String? {
        return withContext(Dispatchers.IO) {
            if (apiKey.isEmpty()) {
                Log.e("GetOnlyAiResponse", "Gemini API key is missing")
                return@withContext null
            }

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", prompt)
                    }))
                }))
            }

            val requestBody = RequestBody.create(
                "application/json".toMediaType(),
                jsonBody.toString()
            )

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            try {
                Log.d("GetOnlyAiResponse", "Sending request to Gemini API")
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("GetOnlyAiResponse", "Raw response: $responseBody")

                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val responseText = try {
                        jsonResponse
                            .getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                    } catch (e: Exception) {
                        Log.e("GetOnlyAiResponse", "Failed to parse response: ${e.message}")
                        null
                    }
                    responseText
                } else {
                    val errorMessage = try {
                        val errorJson = responseBody?.let { JSONObject(it) }
                        errorJson?.getJSONObject("error")?.getString("message") ?: "Unknown error (HTTP ${response.code})"
                    } catch (e: Exception) {
                        responseBody ?: "Unknown error (HTTP ${response.code})"
                    }
                    Log.e("GetOnlyAiResponse", "API request failed: $errorMessage")
                    null
                }
            } catch (e: Exception) {
                Log.e("GetOnlyAiResponse", "Network error: ${e.message}")
                null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }
}