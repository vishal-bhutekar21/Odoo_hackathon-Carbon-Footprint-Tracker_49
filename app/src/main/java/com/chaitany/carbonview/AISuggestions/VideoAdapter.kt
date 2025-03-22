package com.chaitany.carbonview.AISuggestions

import android.content.Intent
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.chaitany.carbonview.R

data class Video(val title: String, val description: String, val link: String)

class VideoAdapter(private val videos: List<Video>) :
    RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvVideoTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvVideoDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        Log.d("VideoAdapter", "Creating ViewHolder for video item")
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = videos[position]
        Log.d("VideoAdapter", "Binding video at position $position: ${video.title}")

        // Set the title (underlined and clickable)
        val spannableTitle = SpannableStringBuilder(video.title)
        spannableTitle.setSpan(UnderlineSpan(), 0, video.title.length, 0)
        holder.tvTitle.text = spannableTitle
        holder.tvTitle.setTextColor(holder.itemView.context.getColor(R.color.teal_600))

        // Set the description
        holder.tvDescription.text = video.description

        // Make the title clickable to open the video link
        holder.tvTitle.setOnClickListener {
            try {
                Log.d("VideoAdapter", "Opening video link: ${video.link}")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.link))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                holder.itemView.context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("VideoAdapter", "Error opening video link: ${video.link}", e)
                Toast.makeText(
                    holder.itemView.context,
                    "Unable to open video link",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun getItemCount(): Int = videos.size
}