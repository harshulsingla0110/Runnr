package com.harshul.runnr.ui.view.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.harshul.runnr.R
import com.harshul.runnr.data.models.Run
import com.harshul.runnr.data.services.Polyline
import com.harshul.runnr.data.services.TrackingService
import com.harshul.runnr.databinding.FragmentTrackingBinding
import com.harshul.runnr.ui.viewmodel.MainViewModel
import com.harshul.runnr.utils.Constants
import com.harshul.runnr.utils.TrackingUtility
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import kotlin.math.round

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    lateinit var binding: FragmentTrackingBinding

    private val viewModel: MainViewModel by viewModels()

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()

    private var map: GoogleMap? = null

    private var currentTimeInMillis = 0L

    @set: Inject
    var weight = 80f

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentTrackingBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        requireActivity().window.apply {
            navigationBarColor = requireActivity().getColor(R.color.dark_mid_grey)
            statusBarColor = requireActivity().getColor(R.color.dark_mid_grey)
            this.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }

        if (savedInstanceState != null) {
            val cancelTrackingDialog =
                parentFragmentManager.findFragmentByTag(Constants.CANCEL_TRACKING_DIALOG_TAG) as CancelTrackingDialog?
            cancelTrackingDialog?.setYesListener { stopRun() }
        }

        binding.btnToggleRun.setOnClickListener {
            toggleRun()
        }

        binding.btnFinishRun.setOnClickListener {
            zoomToSeeWholeTrack()
            endRunAndSaveToDb()
        }

        binding.ivClose.setOnClickListener { if (currentTimeInMillis > 0L) showCancelTrackingDialog() }

        binding.mapView.getMapAsync {
            map = it
            addAllPolylines()
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val pos = LatLng(location.latitude, location.longitude)
                map?.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(pos, 16F)
                )
            }
        }

        subscribeToObservers()

    }

    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })

        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()

            var distanceInMeters = 0f
            for (polyline in pathPoints) {
                distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
            }

            binding.tvDistance.text = "${round((distanceInMeters / 1000) * 100) / 100}"
        })

        TrackingService.timeRunInMillis.observe(viewLifecycleOwner, Observer {
            currentTimeInMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(currentTimeInMillis, true)
            binding.tvTimer.text = formattedTime
        })
    }

    private fun toggleRun() {
        if (isTracking) sendCommandToService(Constants.ACTION_PAUSE_SERVICE)
        else sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE)


    }

    private fun stopRun() {
        sendCommandToService(Constants.ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
    }

    private fun showCancelTrackingDialog() {
        CancelTrackingDialog().apply {
            setYesListener { stopRun() }
        }.show(parentFragmentManager, Constants.CANCEL_TRACKING_DIALOG_TAG)
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if (!isTracking && currentTimeInMillis > 0L) {
            binding.btnToggleRun.text = "Start"
            binding.btnFinishRun.visibility = View.VISIBLE
        } else if (isTracking) {
            binding.btnToggleRun.text = "Stop"
            binding.btnFinishRun.visibility = View.GONE
        }
    }

    private fun moveCameraToUser() {
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    15f
                )
            )
        }
    }

    private fun zoomToSeeWholeTrack() {

        addEndMarker()

        val bounds = LatLngBounds.Builder()
        for (polyline in pathPoints) {
            for (pos in polyline) {
                bounds.include(pos)
            }
        }

        map?.moveCamera( //move camera in stead of animate because right after this we take a screenshot, moves quickly, animate takes time
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                binding.mapView.width,
                binding.mapView.height,
                (binding.mapView.height * 0.15f).toInt() //padding so that boundaries don't coincide with points
            )
        )
    }

    private fun endRunAndSaveToDb() {
        map?.snapshot { bmp ->
            var distanceInMeters = 0
            for (polyline in pathPoints) {
                distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
            }

            val avgSpeed = (distanceInMeters / 1000f) / (currentTimeInMillis / (1000f * 60 * 60))
            val avgSpeedRounded = round(avgSpeed * 10) / 10f
            val dateTimestamp = Calendar.getInstance().timeInMillis
            val caloriesBurned = ((distanceInMeters / 1000f) * weight).toInt()

            val run = Run(
                bmp,
                dateTimestamp,
                avgSpeedRounded,
                distanceInMeters,
                currentTimeInMillis,
                caloriesBurned
            )

            viewModel.insertRun(run)
            stopRun()

        }
    }

    private fun bitmapFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        // below line is use to generate a drawable.
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)

        // below line is use to set bounds to our vector drawable.
        vectorDrawable!!.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )

        // below line is use to create a bitmap for our
        // drawable which we have added.
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        // below line is use to add bitmap in our canvas.
        val canvas = Canvas(bitmap)

        // below line is use to draw our
        // vector drawable in canvas.
        vectorDrawable.draw(canvas)

        // after generating our bitmap we are returning our bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun addStartMarker() {
        val circleOptions = CircleOptions()
            .center(pathPoints[0][0])
            .radius(100.0)
            .fillColor(0x60000000)
            .strokeColor(0x60000000)
            .strokeWidth(0f)
        map?.addCircle(circleOptions)

        map?.addMarker(MarkerOptions().position(pathPoints[0][0]).anchor(0.5f, 0.5f))
            ?.setIcon(bitmapFromVector(requireContext(), R.drawable.marker_start))
    }

    private fun addEndMarker() {

        map?.addMarker(
            MarkerOptions().position(pathPoints.last().last())
        )
            ?.setIcon(bitmapFromVector(requireContext(), R.drawable.marker_finish))
    }

    private fun addAllPolylines() {
        for (polyline in pathPoints) {
            val polylineOptions = PolylineOptions()
                .color(requireContext().getColor(R.color.main_color))
                .width(Constants.MAP_LINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
            addStartMarker()
        }
    }

    private fun addLatestPolyline() {
        if (pathPoints.size == 1 && pathPoints[0].size == 1) addStartMarker()

        if (pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val polylineOptions = PolylineOptions()
                .color(requireContext().getColor(R.color.main_color))
                .width(Constants.MAP_LINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)

            map?.addPolyline(polylineOptions)
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

}