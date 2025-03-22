// com/chaitany/carbonview/AISuggestions/GetAiSuggestions.kt
package com.chaitany.carbonview.AISuggestions

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chaitany.carbonview.R
import com.chaitany.carbonview.databinding.ActivityGetAiSuggestionsBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
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
        Video("How to Reduce Scope 1 Emissions in Manufacturing with Biofuels (2024)", "Learn how manufacturing businesses can switch to biofuels to reduce Scope 1 emissions, with case studies from 2024.", "https://youtu.be/ChT9YNYrfeg?si=pEjjWDsMCQhEld29"),
        Video("Electrifying Your Manufacturing Fleet: A 2024 Guide to Lower Emissions", "A step-by-step guide to transitioning your manufacturing fleet to electric vehicles to cut emissions.", "https://youtu.be/8a4B27IbwiY?si=p1bcAYN1b6I2q_S-"),
        Video("Energy-Efficient Refrigeration for Manufacturing: 2024 Solutions", "Explore the latest ENERGY STAR refrigeration units for manufacturing to reduce device emissions.", "https://youtu.be/miussDactuA?si=ZTmS9YThhKrCLxNY"),
        Video("Smart Thermostats in Manufacturing: Reducing Emissions in 2024", "How smart thermostats can optimize air conditioning usage in manufacturing plants to lower emissions.", "https://youtu.be/js5RA8vTi_A?si=VLxNVyZ3sIqjnnRK"),
        Video("Sustainable Supply Chains in Manufacturing: Reducing Scope 3 Emissions (2024)", "Strategies for partnering with eco-friendly suppliers to reduce Scope 3 emissions in manufacturing.", "https://youtu.be/HM8B0SmVx_I?si=FNXAyHQ7zutXv0Vp"),
        Video("Solar Power for Manufacturing: A 2024 Guide to Reducing Scope 2 Emissions", "Learn how to install solar panels in manufacturing facilities to cut Scope 2 emissions.", "https://youtu.be/3h3ATLqn6tI?si=RFyVWqjSOxolKVaj"),
        Video("Remote Work in Manufacturing: Cutting Scope 3 Emissions in 2024", "How remote work policies can reduce business travel emissions in manufacturing industries.", "https://youtu.be/cJ6ifxHRReA?si=pPynmzfnBJo0OLrH"),
        Video("Bicycle Commutes for Manufacturing Employees: A 2024 Initiative", "Encourage employees to use bicycles for short commutes to reduce Scope 3 emissions.", "https://youtu.be/iMG45Z8Bpzo?si=j_ug-7OwM4f5I999"),
        Video("Waste Reduction in Manufacturing: Lowering Scope 3 Emissions (2024)", "Implement waste reduction programs to lower Scope 3 emissions in manufacturing.", "https://youtu.be/R_ndOiDxrvw?si=QfiUTCZ6WBpJIseJ"),
        Video("Energy Audits for Manufacturing: A 2024 Guide to Efficiency", "Conduct regular energy audits to identify and reduce emissions across all scopes.", "https://youtu.be/aY1jZCL7udo?si=px03KPVPcNazkeTX"),
        Video("Carbon Neutral Manufacturing: 2024 Strategies for Success", "Comprehensive strategies to achieve carbon neutrality in manufacturing by 2025.", "https://youtu.be/NXXORL3Xk4M?si=jjQKDEGKnCtTI35K"),
        Video("Reducing Emissions with IoT in Manufacturing (2024)", "Use IoT devices to monitor and reduce emissions in manufacturing processes.", "https://youtu.be/JKdk6VBM5gk?si=vf_l1eJUxMm_xJ5U"),
        Video("Green Manufacturing Trends for 2024: Lowering Your Carbon Footprint", "Explore the latest trends in green manufacturing to reduce carbon emissions.", "https://youtu.be/sMqtwbKc8EA?si=YwXsu53QwRvSzSth"),
        Video("Decarbonizing Manufacturing: 2024 Best Practices", "Best practices for decarbonizing manufacturing operations in 2024.", "https://youtu.be/X2feS_8RR8Y?si=DvgOnMcDgfJCryo5"),
        Video("Renewable Energy in Manufacturing: 2024 Case Studies", "Case studies on using renewable energy to reduce emissions in manufacturing.", "https://youtu.be/zwsAf0do-yo?si=_MZCl_PfOU0PLtUj"),
        Video("Circular Economy in Manufacturing: Reducing Emissions in 2024", "How a circular economy approach can reduce emissions in manufacturing.", "https://youtu.be/I_G2o1Basvg?si=-rQDYgpIn34oG3T6"),
        Video("Carbon Capture in Manufacturing: 2024 Technologies", "Explore carbon capture technologies for manufacturing to reduce Scope 1 emissions.", "https://youtu.be/XxjNhLZCae0?si=7K0idesS-s-gtcfS"),
        Video("Energy Management Systems for Manufacturing: 2024 Guide", "Use energy management systems to optimize energy use and reduce emissions.", "https://youtu.be/v_dLFbbzvEI?si=bvK9iZ3DT82EMeFQ"),
        Video("Reducing Scope 3 Emissions in Manufacturing Supply Chains (2024)", "Advanced strategies for reducing Scope 3 emissions in manufacturing supply chains.", "https://youtu.be/bzqkDbzsPdY?si=G9CWa3yHIiAut6ZX"),
        Video("Sustainable Manufacturing: A 2024 Roadmap to Net Zero", "A roadmap for manufacturing businesses to achieve net-zero emissions by 2025.", "https://youtu.be/bZmaA8p5fTE?si=G04wPiUHuNaxq2E6")
    )

    private lateinit var mockData: MockData.EmissionData
    private val sections = mutableListOf<Section>()
    private lateinit var loadingDialog: androidx.appcompat.app.AlertDialog
    private lateinit var binding: ActivityGetAiSuggestionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGetAiSuggestionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up the main RecyclerView with an empty adapter
        binding.rvMainContent.layoutManager = LinearLayoutManager(this)
        binding.rvMainContent.adapter = MainContentAdapter(emptyList())

        // Set up the emission summary
        mockData = MockData.getRandomEmissionData()
        val emissionSummaryText = """
            Scope 1: ${String.format("%.2f", mockData.scope1)} kg CO₂
            Scope 2: ${String.format("%.2f", mockData.scope2)} kg CO₂
            Scope 3: ${String.format("%.2f", mockData.scope3)} kg CO₂
            Devices: ${mockData.devices.joinToString { "${it.name}: ${String.format("%.2f", it.emissionKg)} kg" }}
        """.trimIndent()
        binding.tvEmissionSummary.text = emissionSummaryText
        Log.d("GetAiSuggestions", "Mock data set on tvEmissionSummary: $emissionSummaryText")

        // Fetch suggestions
        getSuggestionsFromGemini(mockData, retryCount = 3)
    }

    private fun showLoadingDialog() {
        loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Loading AI Suggestions")
            .setMessage("Fetching AI-powered insights to help reduce your carbon footprint...")
            .setCancelable(false)
            .create()
        loadingDialog.show()
    }

    private fun hideLoadingDialog() {
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    private fun selectRecommendedVideos(data: MockData.EmissionData): List<Video> {
        val recommendedVideos = mutableListOf<Video>()
        val scopes = listOf("Scope 1" to data.scope1, "Scope 2" to data.scope2, "Scope 3" to data.scope3)
        val highestScope = scopes.maxByOrNull { it.second }?.first ?: "Scope 1"
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
        return recommendedVideos.take(2).distinctBy { it.link }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getSuggestionsFromGemini(data: MockData.EmissionData, retryCount: Int) {
        if (!isNetworkAvailable()) {
            runOnUiThread {
                Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show()
                sections.add(Section.EmissionAnalysis(listOf("No internet connection. Please check your network.")))
                binding.rvMainContent.apply {
                    visibility = View.VISIBLE
                    adapter = MainContentAdapter(sections)
                }
                loadVideos()
            }
            return
        }

        if (apiKey.isEmpty()) {
            runOnUiThread {
                Toast.makeText(this, "Gemini API key is missing.", Toast.LENGTH_LONG).show()
                sections.add(Section.EmissionAnalysis(listOf("API key is missing.")))
                binding.rvMainContent.apply {
                    visibility = View.VISIBLE
                    adapter = MainContentAdapter(sections)
                }
                loadVideos()
            }
            return
        }

        runOnUiThread {
            showLoadingDialog()
            Log.d("GetAiSuggestions", "Showing loading dialog")
        }

        val sectionsToFetch = listOf(
            "Emission Analysis" to """
                You are an expert in carbon emission reduction for small-to-medium businesses in the manufacturing industry. Analyze the following carbon emission data for a manufacturing business:

                - Scope 1: ${data.scope1} kg CO₂ (direct emissions from owned or controlled sources, e.g., fuel combustion)
                - Scope 2: ${data.scope2} kg CO₂ (indirect emissions from purchased electricity, steam, heating, or cooling)
                - Scope 3: ${data.scope3} kg CO₂ (other indirect emissions, e.g., business travel, supply chain)
                - Devices: ${data.devices.joinToString { "${it.name} emits ${it.emissionKg} kg CO₂" }}

                **Important**: Use the exact values provided above for Scope 1, Scope 2, Scope 3, and device emissions in your analysis and calculations. Do not modify or assume different values for these data points.

                Provide a detailed response for the following section, formatted for a professional UI with proper alignment, spacing, and typography. Use Markdown for formatting (e.g., *text* for bold, **Section Header** for section headers). Do NOT use HTML tags (e.g., <br>), numbering (e.g., "1.", "2."), or any other non-Markdown formatting. Ensure all paragraphs are separated by blank lines for clarity.

                **${"Emission Analysis"}**:

                - Ensure a blank line between the section header and the first paragraph.
                - Identify which category (Scope 1, Scope 2, or Scope 3) contributes the most emissions and analyze the contribution of each scope.
                - Compare the emissions to typical benchmarks for a small-to-medium manufacturing business (e.g., average Scope 1 emissions are around 3000 kg CO₂ per year).
                - Highlight key contributors among the devices and their impact on the overall emissions. Start this section with *Regarding devices* in bold, followed by a blank line.
                - Ensure each paragraph within this section is separated by a blank line.
            """.trimIndent(),
            "Detailed Suggestions" to """
                You are an expert in carbon emission reduction for small-to-medium businesses in the manufacturing industry. Based on the following carbon emission data for a manufacturing business:

                - Scope 1: ${data.scope1} kg CO₂ (direct emissions from owned or controlled sources, e.g., fuel combustion)
                - Scope 2: ${data.scope2} kg CO₂ (indirect emissions from purchased electricity, steam, heating, or cooling)
                - Scope 3: ${data.scope3} kg CO₂ (other indirect emissions, e.g., business travel, supply chain)
                - Devices: ${data.devices.joinToString { "${it.name} emits ${it.emissionKg} kg CO₂" }}

                **Important**: Use the exact values provided above for Scope 1, Scope 2, Scope 3, and device emissions in your analysis and calculations. Do not modify or assume different values for these data points.

                give me suggetions for reducing this three carbon emmisions """
        )

        CoroutineScope(Dispatchers.IO).launch {
            sections.clear()
            for ((sectionTitle, prompt) in sectionsToFetch) {
                val responseText = fetchSection(prompt, sectionTitle, retryCount)
                if (responseText != null) {
                    val cleanedText = cleanMarkdown(responseText)
                    when (sectionTitle) {
                        "Emission Analysis" -> {
                            val paragraphs = cleanedText.split("\n\n").drop(1)
                            sections.add(Section.EmissionAnalysis(paragraphs))
                        }
                        "Detailed Suggestions" -> {
                            val suggestions = parseDetailedSuggestions(cleanedText)
                            sections.add(Section.DetailedSuggestions(suggestions))
                        }
                        "Additional Insights" -> {
                            val paragraphs = cleanedText.split("\n\n").drop(1)
                            sections.add(Section.AdditionalInsights(paragraphs))
                        }
                        "Resources" -> {
                            val resources = parseResources(cleanedText)
                            sections.add(Section.Resources(resources))
                        }
                    }
                } else {
                    sections.add(Section.EmissionAnalysis(listOf("Failed to load $sectionTitle.")))
                }
            }

            withContext(Dispatchers.Main) {
                hideLoadingDialog()
                Log.d("GetAiSuggestions", "Hiding loading dialog")

                loadVideos()
                binding.rvMainContent.apply {
                    visibility = View.VISIBLE
                    adapter = MainContentAdapter(sections)
                }
            }
        }
    }

    private fun loadVideos() {
        val recommendedVideos = selectRecommendedVideos(mockData)
        sections.add(Section.Videos(recommendedVideos))
        binding.rvMainContent.adapter?.notifyDataSetChanged()
        Log.d("GetAiSuggestions", "Recommended videos added: ${recommendedVideos.size} videos")
    }

    private fun parseDetailedSuggestions(text: String): List<DetailedSuggestion> {
        val suggestions = mutableListOf<DetailedSuggestion>()
        val suggestionBlocks = text.split("\n\n\n").drop(1) // Drop the header

        for (block in suggestionBlocks) {
            val lines = block.split("\n\n").map { it.trim() }
            var action = ""
            var steps = ""
            var impact = ""
            var challenges = ""

            for (line in lines) {
                when {
                    line.startsWith("*Action*:") -> action = line.replace("*Action*:", "").trim()
                    line.startsWith("*Steps*:") -> steps = line.replace("*Steps*:", "").trim()
                    line.startsWith("*Impact*:") -> impact = line.replace("*Impact*:", "").trim()
                    line.startsWith("*Challenges*:") -> challenges = line.replace("*Challenges*:", "").trim()
                }
            }

            // Include the suggestion even if some fields are missing
            if (action.isNotEmpty()) { // At least the action must be present
                suggestions.add(
                    DetailedSuggestion(
                        action = action,
                        steps = steps.ifEmpty { "Steps not provided." },
                        impact = impact.ifEmpty { "Impact not provided." },
                        challenges = challenges.ifEmpty { "Challenges not provided." }
                    )
                )
            }
        }

        return suggestions
    }

    private fun parseResources(text: String): List<Resource> {
        val resources = mutableListOf<Resource>()
        val resourceBlocks = text.split("\n\n").drop(1)

        for (block in resourceBlocks) {
            val urlMatch = Regex("(https?://\\S+?)(?=\\s|$|\\]|\\))").find(block)
            if (urlMatch != null) {
                val url = urlMatch.value
                val description = block.substringBefore(url).trim()
                val titleMatch = description.split(":").first().trim()
                val desc = description.substringAfter(":").trim()
                resources.add(Resource(titleMatch, desc, url))
            }
        }

        return resources
    }

    private suspend fun fetchSection(prompt: String, sectionTitle: String, retryCount: Int): String? {
        return withContext(Dispatchers.IO) {
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
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    Log.d("GetAiSuggestions", "Raw Gemini API response for $sectionTitle: $responseBody")
                    val suggestionsText = try {
                        jsonResponse
                            .getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                    } catch (e: Exception) {
                        Log.e("GetAiSuggestions", "Failed to parse Gemini API response for $sectionTitle: ${e.message}")
                        null
                    }
                    suggestionsText
                } else {
                    val errorMessage = try {
                        val errorJson = responseBody?.let { JSONObject(it) }
                        errorJson?.getJSONObject("error")?.getString("message") ?: "Unknown error (HTTP ${response.code})"
                    } catch (e: Exception) {
                        responseBody ?: "Unknown error (HTTP ${response.code})"
                    }
                    Log.e("GetAiSuggestions", "API request failed for $sectionTitle: $errorMessage")
                    if (response.code == 429 && retryCount > 0) {
                        Log.d("GetAiSuggestions", "Rate limit exceeded for $sectionTitle, retrying... ($retryCount attempts left)")
                        Thread.sleep(1000)
                        fetchSection(prompt, sectionTitle, retryCount - 1)
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("GetAiSuggestions", "Network error for $sectionTitle: ${e.message}")
                null
            }
        }
    }

    private fun cleanMarkdown(text: String): String {
        return text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "*$1*")
            .replace(Regex("\\*\\s+"), "*")
            .replace(Regex("\\s+\\*"), "*")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}

data class Video(val title: String, val description: String, val link: String)

data class DetailedSuggestion(val action: String, val steps: String, val impact: String, val challenges: String)

data class Resource(val title: String, val description: String, val url: String)

sealed class Section {
    data class EmissionAnalysis(val paragraphs: List<String>) : Section()
    data class DetailedSuggestions(val suggestions: List<DetailedSuggestion>) : Section()
    data class AdditionalInsights(val paragraphs: List<String>) : Section()
    data class Resources(val resources: List<Resource>) : Section()
    data class Videos(val videos: List<Video>) : Section()
}

class MainContentAdapter(private val sections: List<Section>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_EMISSION_ANALYSIS = 0
        private const val TYPE_DETAILED_SUGGESTIONS = 1
        private const val TYPE_ADDITIONAL_INSIGHTS = 2
        private const val TYPE_RESOURCES = 3
        private const val TYPE_VIDEOS = 4
    }

    override fun getItemViewType(position: Int): Int {
        return when (sections[position]) {
            is Section.EmissionAnalysis -> TYPE_EMISSION_ANALYSIS
            is Section.DetailedSuggestions -> TYPE_DETAILED_SUGGESTIONS
            is Section.AdditionalInsights -> TYPE_ADDITIONAL_INSIGHTS
            is Section.Resources -> TYPE_RESOURCES
            is Section.Videos -> TYPE_VIDEOS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_EMISSION_ANALYSIS -> EmissionAnalysisViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_emission_analysis, parent, false)
            )
            TYPE_DETAILED_SUGGESTIONS -> DetailedSuggestionsViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_detailed_suggestions, parent, false)
            )
            TYPE_ADDITIONAL_INSIGHTS -> AdditionalInsightsViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_additional_insights, parent, false)
            )
            TYPE_RESOURCES -> ResourcesViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_resources, parent, false)
            )
            TYPE_VIDEOS -> VideosViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_videos, parent, false)
            )
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val section = sections[position]) {
            is Section.EmissionAnalysis -> (holder as EmissionAnalysisViewHolder).bind(section.paragraphs)
            is Section.DetailedSuggestions -> (holder as DetailedSuggestionsViewHolder).bind(section.suggestions)
            is Section.AdditionalInsights -> (holder as AdditionalInsightsViewHolder).bind(section.paragraphs)
            is Section.Resources -> (holder as ResourcesViewHolder).bind(section.resources)
            is Section.Videos -> (holder as VideosViewHolder).bind(section.videos)
        }
    }

    override fun getItemCount(): Int = sections.size
}

class EmissionAnalysisViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val tvHighestScope: TextView = view.findViewById(R.id.tvHighestScope)
    private val tvBenchmark: TextView = view.findViewById(R.id.tvBenchmark)
    private val rvDeviceContributions: RecyclerView = view.findViewById(R.id.rvDeviceContributions)

    fun bind(paragraphs: List<String>) {
        var highestScope = "No data available."
        var benchmark = "No benchmark data available."
        val deviceContributions = mutableListOf<String>()

        paragraphs.forEachIndexed { index, paragraph ->
            when {
                index == 0 -> highestScope = paragraph
                index == 1 -> benchmark = paragraph
                paragraph.startsWith("*Regarding devices*") -> {
                    val devicesText = paragraph.replace("*Regarding devices*", "").trim()
                    deviceContributions.addAll(devicesText.split("\n").map { it.trim() }.filter { it.isNotBlank() })
                }
            }
        }

        tvHighestScope.text = formatText(highestScope)
        tvBenchmark.text = formatText(benchmark)
        rvDeviceContributions.adapter = BulletPointAdapter(deviceContributions)
    }

    private fun formatText(text: String): CharSequence {
        val spannable = SpannableStringBuilder()
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }

        for (line in lines) {
            var currentIndex = 0
            val lineText = SpannableStringBuilder()

            while (currentIndex < line.length) {
                val nextAsterisk = line.indexOf("*", currentIndex)
                if (nextAsterisk == -1) {
                    lineText.append(line.substring(currentIndex))
                    break
                }

                lineText.append(line.substring(currentIndex, nextAsterisk))
                currentIndex = nextAsterisk + 1

                if (currentIndex >= line.length) break

                val endAsterisk = line.indexOf("*", currentIndex)
                if (endAsterisk == -1) {
                    lineText.append(line.substring(currentIndex - 1))
                    break
                }

                val boldText = line.substring(currentIndex, endAsterisk)
                if (boldText.isNotEmpty()) {
                    lineText.append(boldText, StyleSpan(android.graphics.Typeface.BOLD), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                currentIndex = endAsterisk + 1
            }

            if (currentIndex < line.length) {
                lineText.append(line.substring(currentIndex))
            }

            if (spannable.isNotEmpty()) {
                spannable.append("\n")
            }
            spannable.append(lineText)
        }

        return spannable
    }
}

class BulletPointAdapter(private val points: List<String>) :
    RecyclerView.Adapter<BulletPointAdapter.BulletPointViewHolder>() {

    class BulletPointViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBulletPoint: TextView = view.findViewById(R.id.tvBulletPoint)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BulletPointViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bullet_point, parent, false)
        return BulletPointViewHolder(view)
    }

    override fun onBindViewHolder(holder: BulletPointViewHolder, position: Int) {
        val point = points[position]
        holder.tvBulletPoint.text = formatText(point)
    }

    override fun getItemCount(): Int = points.size

    private fun formatText(text: String): CharSequence {
        val spannable = SpannableStringBuilder()
        var currentIndex = 0

        while (currentIndex < text.length) {
            val nextAsterisk = text.indexOf("*", currentIndex)
            if (nextAsterisk == -1) {
                spannable.append(text.substring(currentIndex))
                break
            }

            spannable.append(text.substring(currentIndex, nextAsterisk))
            currentIndex = nextAsterisk + 1

            if (currentIndex >= text.length) break

            val endAsterisk = text.indexOf("*", currentIndex)
            if (endAsterisk == -1) {
                spannable.append(text.substring(currentIndex - 1))
                break
            }

            val boldText = text.substring(currentIndex, endAsterisk)
            if (boldText.isNotEmpty()) {
                spannable.append(boldText, StyleSpan(android.graphics.Typeface.BOLD), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            currentIndex = endAsterisk + 1
        }

        if (currentIndex < text.length) {
            spannable.append(text.substring(currentIndex))
        }

        return spannable
    }
}

class DetailedSuggestionsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val rvSuggestions: RecyclerView = view.findViewById(R.id.rvSuggestions)

    fun bind(suggestions: List<DetailedSuggestion>) {
        rvSuggestions.adapter = SuggestionAdapter(suggestions)
    }
}

class AdditionalInsightsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val rvInsightsPoints: RecyclerView = view.findViewById(R.id.rvInsightsPoints)

    fun bind(paragraphs: List<String>) {
        val points = mutableListOf<String>()
        paragraphs.forEach { paragraph ->
            points.addAll(paragraph.split("\n").map { it.trim() }.filter { it.isNotBlank() })
        }
        rvInsightsPoints.adapter = BulletPointAdapter(points)
    }
}

class ResourcesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val rvResources: RecyclerView = view.findViewById(R.id.rvResources)

    fun bind(resources: List<Resource>) {
        rvResources.adapter = ResourceAdapter(resources)
    }
}

class VideosViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val rvVideos: RecyclerView = view.findViewById(R.id.rvVideos)

    fun bind(videos: List<Video>) {
        rvVideos.adapter = VideoAdapter(videos)
    }
}

class SuggestionAdapter(private val suggestions: List<DetailedSuggestion>) :
    RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder>() {

    class SuggestionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chipAction: Chip = view.findViewById(R.id.chipAction)
        val llDetails: LinearLayout = view.findViewById(R.id.llDetails)
        val tvSteps: TextView = view.findViewById(R.id.tvSteps)
        val tvImpact: TextView = view.findViewById(R.id.tvImpact)
        val tvChallenges: TextView = view.findViewById(R.id.tvChallenges)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        val suggestion = suggestions[position]
        holder.chipAction.text = suggestion.action
        holder.tvSteps.text = suggestion.steps
        holder.tvImpact.text = suggestion.impact
        holder.tvChallenges.text = suggestion.challenges

        // Set initial content description
        holder.chipAction.contentDescription = "Expand suggestion details"

        holder.chipAction.setOnClickListener {
            val isExpanded = holder.llDetails.visibility == View.VISIBLE
            holder.llDetails.visibility = if (isExpanded) View.GONE else View.VISIBLE
            holder.chipAction.chipIcon = holder.itemView.context.getDrawable(
                if (isExpanded) R.drawable.ic_expand_more else R.drawable.ic_expand_less
            )
            // Update content description based on state
            holder.chipAction.contentDescription = if (isExpanded) "Expand suggestion details" else "Collapse suggestion details"
        }
    }

    override fun getItemCount(): Int = suggestions.size
}

class ResourceAdapter(private val resources: List<Resource>) :
    RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder>() {

    class ResourceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvResourceTitle: TextView = view.findViewById(R.id.tvResourceTitle)
        val tvResourceDescription: TextView = view.findViewById(R.id.tvResourceDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_resource, parent, false)
        return ResourceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResourceViewHolder, position: Int) {
        val resource = resources[position]
        holder.tvResourceTitle.text = resource.title
        holder.tvResourceDescription.text = resource.description

        holder.itemView.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resource.url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                holder.itemView.context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    holder.itemView.context,
                    "Unable to open link",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun getItemCount(): Int = resources.size
}

class VideoAdapter(private val videos: List<Video>) :
    RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.tvTitle.text = video.title
        holder.tvDescription.text = video.description

        holder.itemView.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.link))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                holder.itemView.context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    holder.itemView.context,
                    "Unable to open video",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun getItemCount(): Int = videos.size
}