package com.harshul.runnr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.harshul.runnr.data.models.Run
import com.harshul.runnr.databinding.ItemRunBinding
import com.harshul.runnr.utils.TrackingUtility
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round

class RunAdapter : RecyclerView.Adapter<RunAdapter.RunViewHolder>() {

    val diffCallback = object : DiffUtil.ItemCallback<Run>() {
        override fun areItemsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(list: List<Run>) = differ.submitList(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunViewHolder {
        val binding = ItemRunBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RunViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RunViewHolder, position: Int) {

        val run = differ.currentList[position]

        with(holder) {
            Glide.with(holder.itemView).load(run.img).into(binding.ivRunImage)

            val calendar = Calendar.getInstance().apply {
                timeInMillis = run.timestamp
            }
            val dateFormat = SimpleDateFormat("hh:mm aaa, E, dd MMM yy", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(calendar.time)

            val avgSpeed = "${run.avgSpeedInKMH}km/h"
            binding.tvAvgSpeed.text = avgSpeed

            val distanceInKm = "${round(run.distanceInMeters / 1000f * 100) / 100}km Morning Run"
            //val distanceInKm = "${run.distanceInMeters / 1000f }km Morning Run"
            binding.tvDistance.text = distanceInKm

            binding.tvTime.text = TrackingUtility.getFormattedStopWatchTime(run.timeInMillis)

            val caloriesBurned = "${run.caloriesBurned}kcal"
            binding.tvCalories.text = caloriesBurned

        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    inner class RunViewHolder(val binding: ItemRunBinding) : RecyclerView.ViewHolder(binding.root)

}