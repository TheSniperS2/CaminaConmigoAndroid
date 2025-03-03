package com.franco.CaminaConmigo.model_mvvm.chat.view

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.franco.CaminaConmigo.R
import com.franco.CaminaConmigo.databinding.FragmentSharedLocationMapViewBinding
import com.franco.CaminaConmigo.model_mvvm.chat.model.LocationMessage
import com.franco.CaminaConmigo.model_mvvm.chat.viewmodel.LocationSharingViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class SharedLocationMapViewFragment : Fragment(R.layout.fragment_shared_location_map_view), OnMapReadyCallback {

    private val locationSharingViewModel: LocationSharingViewModel by viewModels()
    private var _binding: FragmentSharedLocationMapViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapView: MapView
    private var map: GoogleMap? = null
    private lateinit var locationMessage: LocationMessage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            locationMessage = it.getParcelable("locationMessage")!!
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSharedLocationMapViewBinding.bind(view)
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        binding.btnShowFullScreen.setOnClickListener {
            // Handle showing full screen map
        }

        locationSharingViewModel.activeLocationSharing.observe(viewLifecycleOwner, { locationMap ->
            locationMap[locationMessage.senderId]?.let { updatedLocation ->
                updateMapLocation(updatedLocation.latitude, updatedLocation.longitude)
            }
        })
    }

    private fun updateMapLocation(latitude: Double, longitude: Double) {
        map?.let {
            val location = LatLng(latitude, longitude)
            it.clear()
            it.addMarker(MarkerOptions().position(location).title("Ubicación compartida"))
            it.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val location = LatLng(locationMessage.latitude, locationMessage.longitude)
        googleMap.addMarker(MarkerOptions().position(location).title("Ubicación compartida"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    companion object {
        @JvmStatic
        fun newInstance(locationMessage: LocationMessage) = SharedLocationMapViewFragment().apply {
            arguments = Bundle().apply {
                putParcelable("locationMessage", locationMessage)
            }
        }
    }
}