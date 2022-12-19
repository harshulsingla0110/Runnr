package com.harshul.runnr.ui.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.harshul.runnr.R
import com.harshul.runnr.databinding.FragmentStatisticsBinding
import com.harshul.runnr.ui.viewmodel.StatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import kotlin.math.round

@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    lateinit var binding: FragmentStatisticsBinding
    private val viewModel: StatisticsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStatisticsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()
    }

    private fun subscribeToObservers() {

        viewModel.totaltimeRun.observe(viewLifecycleOwner, Observer { timeInMillis ->
            timeInMillis?.let {
                var milliseconds = timeInMillis
                val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
                milliseconds -= TimeUnit.HOURS.toMillis(hours)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
                milliseconds -= TimeUnit.MINUTES.toMillis(minutes)

                val totalTimeRun = "${hours}h ${minutes}m"
                if (hours < 10) binding.tvTime.text = totalTimeRun
                else "${hours}h".also { binding.tvTime.text = it }

            }
        })

        viewModel.totalDistance.observe(viewLifecycleOwner, Observer {
            it?.let {
                val km = it / 1000f
                val totalDistance = round(km * 10f) / 10f
                binding.tvDistance.text = "$totalDistance"
            }
        })

        viewModel.totalCaloriesBurned.observe(viewLifecycleOwner, Observer {
            it?.let {
                binding.tvCalorie.text = "$it"
            }
        })

        viewModel.totalAvgSpeed.observe(viewLifecycleOwner, Observer {
            it?.let {
                val avgSpeed = round(it * 10f) / 10f
                binding.tvSpeed.text = "$avgSpeed"
            }
        })


    }

}