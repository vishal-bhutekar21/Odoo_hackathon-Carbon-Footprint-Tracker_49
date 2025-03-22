package com.chaitany.carbonview.AISuggestions

import android.content.Intent
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.chaitany.carbonview.R

data class Suggestion(val text: String, val link: String? = null)

class SuggestionAdapter(private var suggestions: List<Suggestion>) :
    RecyclerView.Adapter<SuggestionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSuggestion: TextView = view.findViewById(R.id.tvSuggestion)
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion, parent, false)
        Log.d("SuggestionAdapter", "Creating ViewHolder for item")
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val suggestion = suggestions[position]
        val text = suggestion.text.trim()

        Log.d("SuggestionAdapter", "Binding item at position $position: text=$text, link=${suggestion.link}")

        if (suggestion.link?.contains("http") == true) {
            // Handle article links
            val linkTitle = text.removePrefix("1.").removePrefix("2.").removePrefix("3.").trim()
            val cleanedLinkTitle = if (linkTitle.isNotEmpty()) linkTitle else "Resource Link"
            val linkUrl = suggestion.link

            // Apply underline and color to the link title
            val spannable = SpannableStringBuilder(cleanedLinkTitle)
            spannable.setSpan(UnderlineSpan(), 0, cleanedLinkTitle.length, 0)
            holder.tvSuggestion.text = spannable
            holder.tvSuggestion.setTextColor(holder.itemView.context.getColor(R.color.teal_600))
            holder.tvSuggestion.visibility = View.VISIBLE
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_info_details)
            holder.ivIcon.visibility = View.VISIBLE

            // Make the link clickable
            holder.tvSuggestion.setOnClickListener {
                if (linkUrl != null && linkUrl.startsWith("http")) {
                    try {
                        Log.d("SuggestionAdapter", "Opening link: $linkUrl")
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        holder.itemView.context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("SuggestionAdapter", "Error opening link: $linkUrl", e)
                        Toast.makeText(
                            holder.itemView.context,
                            "Unable to open link",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.w("SuggestionAdapter", "Invalid URL: $linkUrl")
                }
            }
        } else {
            // Handle regular text with Markdown parsing for bold and line breaks
            val formattedText = parseMarkdown(text)
            holder.tvSuggestion.text = formattedText
            holder.tvSuggestion.setTextColor(holder.itemView.context.getColor(R.color.text_primary))
            holder.tvSuggestion.visibility = View.VISIBLE
            holder.ivIcon.visibility = View.GONE
            Log.d("SuggestionAdapter", "Set regular text: $formattedText")
        }
    }

    override fun getItemCount(): Int = suggestions.size

    private fun parseMarkdown(text: String): CharSequence {
        Log.d("SuggestionAdapter", "Parsing Markdown for text: $text")
        val spannable = SpannableStringBuilder()
        val lines = text.split("\n").map { it.trim() }

        var isInSuggestion = false // Track if we're in the "Detailed Suggestions" section
        var isAfterRegardingDevices = false // Track if we're after "Regarding devices"

        for (line in lines) {
            if (line.isBlank()) {
                spannable.append("\n\n") // Add extra line break for blank lines
                continue
            }

            // Check if the line is a section header (starts with ** and ends with **)
            if (line.matches(Regex("^\\*\\*[^\\*]+\\*\\*$"))) {
                // Remove numbering (e.g., "1.", "2.") from the start of the header
                var headerText = line.replace(Regex("\\*\\*"), "").trim()
                headerText = headerText.replace(Regex("^\\d+\\.\\s*"), "").trim()
                if (spannable.isNotEmpty()) {
                    spannable.append("\n\n") // Add extra spacing before a new section
                }
                spannable.append(headerText, StyleSpan(android.graphics.Typeface.BOLD), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.append("\n\n") // Add extra spacing after header
                isInSuggestion = headerText.contains("Detailed Suggestions", ignoreCase = true)
                isAfterRegardingDevices = false
            } else {
                // Parse Markdown for bold text in the line
                var currentIndex = 0
                val lineText = SpannableStringBuilder()
                var isBoldLine = false

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
                        // Remove duplicates (e.g., "Steps Steps" to "Steps")
                        val cleanedBoldText = boldText.replace(Regex("^(Steps|Impact|Challenges)\\s+\\1"), "$1")
                        lineText.append(cleanedBoldText, StyleSpan(android.graphics.Typeface.BOLD), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                        isBoldLine = true
                    }
                    currentIndex = endAsterisk + 1
                }

                // Add the remaining text after the last bold section
                if (currentIndex < line.length) {
                    lineText.append(line.substring(currentIndex))
                }

                // Add spacing based on context
                if (spannable.isNotEmpty()) {
                    if (isBoldLine || isAfterRegardingDevices) {
                        spannable.append("\n\n") // Add extra spacing after bold lines or "Regarding devices"
                    } else {
                        spannable.append("\n") // Add single line break within a paragraph
                    }
                }

                spannable.append(lineText)

                // In the "Detailed Suggestions" section, add extra spacing after "Challenges"
                if (isInSuggestion && line.startsWith("*Challenges*")) {
                    spannable.append("\n\n") // Add extra spacing after each suggestion
                }

                // Set flag after encountering "Regarding devices"
                if (line.startsWith("*Regarding devices*")) {
                    isAfterRegardingDevices = true
                } else {
                    isAfterRegardingDevices = false
                }
            }
        }

        return spannable
    }
}