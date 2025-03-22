package com.chaitany.carbonview.AISuggestions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chaitany.carbonview.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GetAiSuggestions : AppCompatActivity() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    private val apiKey: String by lazy { getString(R.string.gemini_api_key) }

    // Pool of YouTube videos with real links (as of March 2025, based on trends)
    private val videoPool = listOf(
        Video(
            title = "How to Reduce Scope 1 Emissions in Manufacturing with Biofuels (2024)",
            description = "Learn how manufacturing businesses can switch to biofuels to reduce Scope 1 emissions, with case studies from 2024.",
            link = "https://youtu.be/ChT9YNYrfeg?si=pEjjWDsMCQhEld29"
        ),
        Video(
            title = "Electrifying Your Manufacturing Fleet: A 2024 Guide to Lower Emissions",
            description = "A step-by-step guide to transitioning your manufacturing fleet to electric vehicles to cut emissions.",
            link = "https://youtu.be/8a4B27IbwiY?si=p1bcAYN1b6I2q_S-"
        ),
        Video(
            title = "Energy-Efficient Refrigeration for Manufacturing: 2024 Solutions",
            description = "Explore the latest ENERGY STAR refrigeration units for manufacturing to reduce device emissions.",
            link = "https://youtu.be/miussDactuA?si=ZTmS9YThhKrCLxNY"
        ),
        Video(
            title = "Smart Thermostats in Manufacturing: Reducing Emissions in 2024",
            description = "How smart thermostats can optimize air conditioning usage in manufacturing plants to lower emissions.",
            link = "https://youtu.be/js5RA8vTi_A?si=VLxNVyZ3sIqjnnRK"
        ),
        Video(
            title = "Sustainable Supply Chains in Manufacturing: Reducing Scope 3 Emissions (2024)",
            description = "Strategies for partnering with eco-friendly suppliers to reduce Scope 3 emissions in manufacturing.",
            link = "https://youtu.be/HM8B0SmVx_I?si=FNXAyHQ7zutXv0Vp"
        ),
        Video(
            title = "Solar Power for Manufacturing: A 2024 Guide to Reducing Scope 2 Emissions",
            description = "Learn how to install solar panels in manufacturing facilities to cut Scope 2 emissions.",
            link = "https://youtu.be/3h3ATLqn6tI?si=RFyVWqjSOxolKVaj"
        ),
        Video(
            title = "Remote Work in Manufacturing: Cutting Scope 3 Emissions in 2024",
            description = "How remote work policies can reduce business travel emissions in manufacturing industries.",
            link = "https://youtu.be/cJ6ifxHRReA?si=pPynmzfnBJo0OLrH"
        ),
        Video(
            title = "Bicycle Commutes for Manufacturing Employees: A 2024 Initiative",
            description = "Encourage employees to use bicycles for short commutes to reduce Scope 3 emissions.",
            link = "https://youtu.be/iMG45Z8Bpzo?si=j_ug-7OwM4f5I999"
        ),
        Video(
            title = "Waste Reduction in Manufacturing: Lowering Scope 3 Emissions (2024)",
            description = "Implement waste reduction programs to lower Scope 3 emissions in manufacturing.",
            link = "https://youtu.be/R_ndOiDxrvw?si=QfiUTCZ6WBpJIseJ"
        ),
        Video(
            title = "Energy Audits for Manufacturing: A 2024 Guide to Efficiency",
            description = "Conduct regular energy audits to identify and reduce emissions across all scopes.",
            link = "https://youtu.be/aY1jZCL7udo?si=px03KPVPcNazkeTX"
        ),
        Video(
            title = "Carbon Neutral Manufacturing: 2024 Strategies for Success",
            description = "Comprehensive strategies to achieve carbon neutrality in manufacturing by 2025.",
            link = "https://youtu.be/NXXORL3Xk4M?si=jjQKDEGKnCtTI35K"
        ),
        Video(
            title = "Reducing Emissions with IoT in Manufacturing (2024)",
            description = "Use IoT devices to monitor and reduce emissions in manufacturing processes.",
            link = "https://youtu.be/JKdk6VBM5gk?si=vf_l1eJUxMm_xJ5U"
        ),
        Video(
            title = "Green Manufacturing Trends for 2024: Lowering Your Carbon Footprint",
            description = "Explore the latest trends in green manufacturing to reduce carbon emissions.",
            link = "https://youtu.be/sMqtwbKc8EA?si=YwXsu53QwRvSzSth"
        ),
        Video(
            title = "Decarbonizing Manufacturing: 2024 Best Practices",
            description = "Best practices for decarbonizing manufacturing operations in 2024.",
            link = "https://youtu.be/X2feS_8RR8Y?si=DvgOnMcDgfJCryo5"
        ),
        Video(
            title = "Renewable Energy in Manufacturing: 2024 Case Studies",
            description = "Case studies on using renewable energy to reduce emissions in manufacturing.",
            link = "https://youtu.be/zwsAf0do-yo?si=_MZCl_PfOU0PLtUj"
        ),
        Video(
            title = "Circular Economy in Manufacturing: Reducing Emissions in 2024",
            description = "How a circular economy approach can reduce emissions in manufacturing.",
            link = "https://youtu.be/I_G2o1Basvg?si=-rQDYgpIn34oG3T6"
        ),
        Video(
            title = "Carbon Capture in Manufacturing: 2024 Technologies",
            description = "Explore carbon capture technologies for manufacturing to reduce Scope 1 emissions.",
            link = "https://youtu.be/XxjNhLZCae0?si=7K0idesS-s-gtcfS"
        ),
        Video(
            title = "Energy Management Systems for Manufacturing: 2024 Guide",
            description = "Use energy management systems to optimize energy use and reduce emissions.",
            link = "https://youtu.be/v_dLFbbzvEI?si=bvK9iZ3DT82EMeFQ"
        ),
        Video(
            title = "Reducing Scope 3 Emissions in Manufacturing Supply Chains (2024)",
            description = "Advanced strategies for reducing Scope 3 emissions in manufacturing supply chains.",
            link = "https://youtu.be/bzqkDbzsPdY?si=G9CWa3yHIiAut6ZX"
        ),
        Video(
            title = "Sustainable Manufacturing: A 2024 Roadmap to Net Zero",
            description = "A roadmap for manufacturing businesses to achieve net-zero emissions by 2025.",
            link = "https://youtu.be/bZmaA8p5fTE?si=G04wPiUHuNaxq2E6"
        )
    )

    private lateinit var mockData: MockData.EmissionData // Store mock data for use in video selection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_get_ai_suggestions)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up RecyclerView for Gemini suggestions
        val rvSuggestions = findViewById<RecyclerView>(R.id.rvSuggestions)
        rvSuggestions.layoutManager = LinearLayoutManager(this)
        rvSuggestions.adapter = SuggestionAdapter(emptyList())
        Log.d("GetAiSuggestions", "RecyclerView (rvSuggestions) initialized with empty adapter")

        // Set up RecyclerView for YouTube video recommendations (initially empty)
        val rvVideos = findViewById<RecyclerView>(R.id.rvVideos)
        rvVideos.layoutManager = LinearLayoutManager(this)
        rvVideos.adapter = VideoAdapter(emptyList())
        Log.d("GetAiSuggestions", "RecyclerView (rvVideos) initialized with empty adapter")

        // Fetch and display mock data and suggestions
        fetchRandomDataAndGetSuggestions()
    }

    private fun fetchRandomDataAndGetSuggestions() {
        // Generate and display mock data immediately
        mockData = MockData.getRandomEmissionData()
        val tvEmissionSummary = findViewById<TextView>(R.id.tvEmissionSummary)
        val emissionSummaryText = """
            Scope 1: ${String.format("%.2f", mockData.scope1)} kg CO₂
            Scope 2: ${String.format("%.2f", mockData.scope2)} kg CO₂
            Scope 3: ${String.format("%.2f", mockData.scope3)} kg CO₂
            Devices: ${mockData.devices.joinToString { "${it.name}: ${String.format("%.2f", it.emissionKg)} kg" }}
        """.trimIndent()
        tvEmissionSummary.text = emissionSummaryText
        Log.d("GetAiSuggestions", "Mock data set on tvEmissionSummary: $emissionSummaryText")

        // Start API call to fetch suggestions
        getSuggestionsFromGemini(mockData, retryCount = 3)
    }

    private fun selectRecommendedVideos(data: MockData.EmissionData): List<Video> {
        val recommendedVideos = mutableListOf<Video>()

        // Determine the highest emission category
        val scopes = listOf(
            "Scope 1" to data.scope1,
            "Scope 2" to data.scope2,
            "Scope 3" to data.scope3
        )
        val highestScope = scopes.maxByOrNull { it.second }?.first ?: "Scope 1"
        val highestDevice = data.devices.maxByOrNull { it.emissionKg }?.name ?: "Vehicle"

        // Select exactly 2 videos based on the highest emission category
        when (highestScope) {
            "Scope 1" -> {
                recommendedVideos.add(videoPool.find { it.title.contains("Biofuels") }!!)
                recommendedVideos.add(videoPool.find { it.title.contains("Carbon Capture") }!!)
            }
            "Scope 2" -> {
                recommendedVideos.add(videoPool.find { it.title.contains("Solar Power") }!!)
                recommendedVideos.add(videoPool.find { it.title.contains("Renewable Energy") }!!)
            }
            "Scope 3" -> {
                recommendedVideos.add(videoPool.find { it.title.contains("Sustainable Supply Chains") }!!)
                recommendedVideos.add(videoPool.find { it.title.contains("Remote Work") }!!)
            }
        }

        // Limit to exactly 2 videos
        return recommendedVideos.take(2).distinctBy { it.link }
    }

    private fun loadVideos() {
        val recommendedVideos = selectRecommendedVideos(mockData)
        val rvVideos = findViewById<RecyclerView>(R.id.rvVideos)
        rvVideos.adapter = VideoAdapter(recommendedVideos)
        Log.d("GetAiSuggestions", "Recommended videos set on rvVideos: ${recommendedVideos.size} videos")
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    private fun getSuggestionsFromGemini(data: MockData.EmissionData, retryCount: Int) {
        if (!isNetworkAvailable()) {
            runOnUiThread {
                Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show()
                val rvSuggestions = findViewById<RecyclerView>(R.id.rvSuggestions)
                rvSuggestions.adapter = SuggestionAdapter(listOf(Suggestion("No internet connection. Please check your network.")))
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                loadVideos() // Load videos even if API fails
            }
            return
        }

        if (apiKey.isEmpty()) {
            runOnUiThread {
                Toast.makeText(this, "Gemini API key is missing.", Toast.LENGTH_LONG).show()
                val rvSuggestions = findViewById<RecyclerView>(R.id.rvSuggestions)
                rvSuggestions.adapter = SuggestionAdapter(listOf(Suggestion("API key is missing.")))
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                loadVideos() // Load videos even if API fails
            }
            return
        }

        val prompt = """
            You are an expert in carbon emission reduction for small-to-medium businesses in the manufacturing industry. Analyze the following carbon emission data for a manufacturing business:

            - Scope 1: ${data.scope1} kg CO₂ (direct emissions from owned or controlled sources, e.g., fuel combustion)
            - Scope 2: ${data.scope2} kg CO₂ (indirect emissions from purchased electricity, steam, heating, or cooling)
            - Scope 3: ${data.scope3} kg CO₂ (other indirect emissions, e.g., business travel, supply chain)
            - Devices: ${data.devices.joinToString { "${it.name} emits ${it.emissionKg} kg CO₂" }}

            **Important**: Use the exact values provided above for Scope 1, Scope 2, Scope 3, and device emissions in your analysis and calculations. Do not modify or assume different values for these data points.

            Provide a detailed response with the following sections:

            **Emission Analysis**:
            - Identify which category (Scope 1, Scope 2, or Scope 3) contributes the most emissions and analyze the contribution of each scope.
            - Compare the emissions to typical benchmarks for a small-to-medium manufacturing business (e.g., average Scope 1 emissions are around 3000 kg CO₂ per year).
            - Highlight key contributors among the devices and their impact on the overall emissions. Start this section with *Regarding devices* in bold, followed by a new line.
            - Ensure each paragraph in this section is separated by a blank line for clarity.

            **Detailed Suggestions**:
            - Provide exactly 10 specific, actionable suggestions to reduce emissions, focusing on the highest emission category and the devices with significant emissions.
            - For each suggestion, include:
              - *Action*: A clear action in bold (e.g., *Switch to LED Lighting*), followed by a new line.
              - *Steps*: Steps to implement the action in bold (e.g., *Steps*: Conduct an energy audit, purchase LED bulbs, hire an electrician for installation), followed by a new line.
              - *Impact*: Potential impact in bold (e.g., *Impact*: This could reduce Scope 2 emissions by 10-15% annually), followed by a new line.
              - *Challenges*: Possible challenges in bold (e.g., *Challenges*: Initial costs for LED bulbs may be high, but savings will be realized within 1-2 years), followed by a new line.
            - Ensure each suggestion is separated by a blank line.
            - Ensure there is a blank line before the first suggestion.

            **Additional Insights**:
            - Provide one additional insight, such as a long-term benefit of reducing emissions (e.g., cost savings, regulatory compliance, improved brand reputation) or a trend in the manufacturing industry related to carbon reduction.
            - Ensure each paragraph in this section is separated by a blank line.

            **Resources**:
            - Provide exactly 2 article links about reducing emissions in the manufacturing industry, with a brief description (e.g., "Guide on Energy Efficiency: https://example.com").
            - The articles must be from well-known, reliable sources (e.g., U.S. Department of Energy, EPA, McKinsey, or similar) and must be accessible as of March 2025.
            - Do NOT include YouTube video links, as these will be provided separately.
            - Ensure each resource entry is separated by a blank line.

            Use Markdown for bold text (e.g., *text* for bold) and section headers (e.g., **Emission Analysis**). Do NOT add numbering (e.g., "1.", "2.") before section headers. Do NOT use HTML tags like <br> in the response. Ensure the response is detailed, actionable, and tailored to a manufacturing business. The response should be at least 500 words to provide sufficient depth.
        """.trimIndent()

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

        val rvSuggestions = findViewById<RecyclerView>(R.id.rvSuggestions)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Show ProgressBar while fetching suggestions
        runOnUiThread {
            progressBar.visibility = View.VISIBLE
            Log.d("GetAiSuggestions", "ProgressBar set to VISIBLE")
        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("GetAiSuggestions", "Network error: ${e.message}")
                    Toast.makeText(
                        this@GetAiSuggestions,
                        "Network Error: ${e.message}. Please check your connection and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    progressBar.visibility = View.GONE
                    rvSuggestions.adapter = SuggestionAdapter(listOf(Suggestion("Failed to load suggestions due to a network error.")))
                    loadVideos() // Load videos after API failure
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Log.d("GetAiSuggestions", "ProgressBar set to GONE after response")
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            Log.d("GetAiSuggestions", "Raw Gemini API response: $responseBody")

                            val suggestionsText = try {
                                jsonResponse
                                    .getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text")
                            } catch (e: Exception) {
                                Log.e("GetAiSuggestions", "Error parsing JSON response: $e")
                                Toast.makeText(this@GetAiSuggestions, "Error parsing API response", Toast.LENGTH_LONG).show()
                                rvSuggestions.adapter = SuggestionAdapter(listOf(Suggestion("Error parsing API response.")))
                                loadVideos() // Load videos even if parsing fails
                                return@runOnUiThread
                            }

                            val cleanedSuggestionsText = cleanMarkdown(suggestionsText)
                            Log.d("GetAiSuggestions", "Cleaned suggestions text: $cleanedSuggestionsText")

                            // Parse the response into sections
                            val suggestionList = mutableListOf<Suggestion>()
                            val sections = cleanedSuggestionsText.split("(?=^\\*\\*[^\\*]+\\*\\*$)".toRegex()).filter { it.isNotBlank() }

                            for (section in sections) {
                                val lines = section.split("\n").filter { it.isNotBlank() }
                                if (lines.isEmpty()) continue

                                val sectionTitle = lines[0].replace(Regex("\\*\\*"), "").trim()
                                val sectionContent = StringBuilder()

                                for (line in lines.drop(1)) {
                                    val urlMatch = Regex("(https?://\\S+?)(?=\\s|$|\\]|\\))").find(line)
                                    if (urlMatch != null) {
                                        val url = urlMatch.value
                                        val articleDescription = line.substringBefore(url).replace(Regex("\\[.*\\]"), "").trim()
                                        val articleText = if (articleDescription.isNotEmpty()) articleDescription else "Resource Link"
                                        suggestionList.add(Suggestion(articleText, url))
                                    } else {
                                        if (sectionContent.isNotEmpty()) {
                                            sectionContent.append("\n")
                                        }
                                        sectionContent.append(line)
                                    }
                                }

                                if (sectionContent.isNotEmpty()) {
                                    suggestionList.add(Suggestion("$sectionTitle\n${sectionContent.toString().trim()}"))
                                }
                            }

                            if (suggestionList.isEmpty()) {
                                Log.w("GetAiSuggestions", "No suggestions parsed from API response.")
                                suggestionList.add(Suggestion("No suggestions available. Please try again later."))
                            }

                            Log.d("GetAiSuggestions", "Parsed suggestion list: $suggestionList")

                            val adapter = SuggestionAdapter(suggestionList)
                            rvSuggestions.adapter = adapter
                            adapter.notifyDataSetChanged()
                            Log.d("GetAiSuggestions", "RecyclerView adapter updated with ${suggestionList.size} items.")
                            rvSuggestions.visibility = View.VISIBLE
                            Log.d("GetAiSuggestions", "RecyclerView visibility set to VISIBLE")

                            // Load videos after suggestions are displayed
                            loadVideos()
                        } catch (e: Exception) {
                            Log.e("GetAiSuggestions", "Error processing API response: $e")
                            Toast.makeText(this@GetAiSuggestions, "Error processing API response: ${e.message}", Toast.LENGTH_LONG).show()
                            rvSuggestions.adapter = SuggestionAdapter(listOf(Suggestion("Error processing API response.")))
                            loadVideos() // Load videos even if processing fails
                        }
                    } else {
                        val errorMessage = try {
                            val errorJson = responseBody?.let { JSONObject(it) }
                            errorJson?.getJSONObject("error")?.getString("message") ?: "Unknown error (HTTP ${response.code})"
                        } catch (e: Exception) {
                            responseBody ?: "Unknown error (HTTP ${response.code})"
                        }
                        Log.e("GetAiSuggestions", "API request failed: $errorMessage")
                        if (response.code == 429 && retryCount > 0) {
                            Log.d("GetAiSuggestions", "Rate limit exceeded, retrying... ($retryCount attempts left)")
                            Thread.sleep(1000)
                            getSuggestionsFromGemini(data, retryCount - 1)
                        } else {
                            Toast.makeText(this@GetAiSuggestions, "Failed to get suggestions: $errorMessage", Toast.LENGTH_LONG).show()
                            rvSuggestions.adapter = SuggestionAdapter(listOf(Suggestion("Failed to load suggestions: $errorMessage")))
                            loadVideos() // Load videos after API failure
                        }
                    }
                }
            }
        })
    }

    private fun cleanMarkdown(text: String): String {
        return text.replace(Regex("\\*\\*(.*?)\\*\\*"), "*$1*")
            .replace(Regex("\\*\\s+"), "*")
            .replace(Regex("\\s+\\*"), "*")
            .replace(Regex("<br\\s*/?>"), "\n") // Remove <br> tags and replace with newlines
            .replace(Regex("\n{3,}"), "\n\n") // Normalize multiple newlines to double newlines
    }
}