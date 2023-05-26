package androidkotlin.formation.endormap.map

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidkotlin.formation.endormap.R
import androidkotlin.formation.endormap.databinding.ActivityMapBinding
import androidkotlin.formation.endormap.geofence.GEOFENCE_ID_MORDOR
import androidkotlin.formation.endormap.geofence.GeofenceManager
import androidkotlin.formation.endormap.location.LocationData
import androidkotlin.formation.endormap.location.LocationLiveData
import androidkotlin.formation.endormap.poi.MOUNT_DOOM
import androidkotlin.formation.endormap.poi.Poi
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import timber.log.Timber

private const val REQUEST_PERMISSION_NOTIFICATION = 3
private const val REQUEST_PERMISSION_LOCATION_START_UPDATE = 2
private const val REQUEST_CHECK_SETTINGS = 1

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var viewModel: MapViewModel
    private lateinit var locationLiveData: LocationLiveData

    private lateinit var binding: ActivityMapBinding

    private var firstLocation = true
    private lateinit var map: GoogleMap
    private lateinit var userMarker: Marker
    private lateinit var geofenceManager: GeofenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val mapOptions = GoogleMapOptions()
            .mapType(GoogleMap.MAP_TYPE_NORMAL)
            .zoomControlsEnabled(true)
            .zoomGesturesEnabled(true)

        val mapFragment = SupportMapFragment.newInstance(mapOptions)
        mapFragment.getMapAsync(this)

        supportFragmentManager.beginTransaction()
            .replace(R.id.content, mapFragment)
            .commit()

        geofenceManager = GeofenceManager(this)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ) {
            checkNotificationPermission(REQUEST_PERMISSION_NOTIFICATION)
        }

        locationLiveData = LocationLiveData(this)
        locationLiveData.observe(this) {
            handleLocationData(it!!)
        }

        viewModel = ViewModelProvider(this)[MapViewModel::class.java]
        viewModel.getUiState().observe(this) {
            updateUiState(it!!)
        }
    }

    private fun updateUiState(state: MapUiState) {
        Timber.i("$state")
        return when(state) {
            MapUiState.Loading -> binding.loadingProgressBar.show()
            is MapUiState.Error -> {
                binding.loadingProgressBar.hide()
                Toast.makeText(this, "Error: ${state.errorMessage}", Toast.LENGTH_SHORT).show()
            }
            is MapUiState.PoiReady -> {
                binding.loadingProgressBar.hide()

                state.userPoi?.let { poi ->
                    userMarker = addPoiToMapMarker(poi, map)
                }
                state.pois?.let { pois ->
                    for(poi in pois) {
                        addPoiToMapMarker(poi, map)

                        if(poi.title == MOUNT_DOOM) {
                            geofenceManager.createGeofence(poi, 10000.0F, GEOFENCE_ID_MORDOR)
                        }
                    }
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            REQUEST_CHECK_SETTINGS -> locationLiveData.startRequestLocation()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_map_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.generate_pois -> {
                refreshPoisFromCurrentLocation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshPoisFromCurrentLocation() {
        geofenceManager.removeAllGeofences()
        map.clear()
        viewModel.loadPois(userMarker.position.latitude, userMarker.position.longitude)
    }

    private fun handleLocationData(locationData: LocationData) {
        if(handleLocationException(locationData.exception)) {
            return
        }

        locationData.location?.let {
            val latLng = LatLng(it.latitude, it.longitude)

            if(firstLocation && ::map.isInitialized) {

                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 9F))

                firstLocation = false
                viewModel.loadPois(it.latitude, it.longitude)
            }

            if(::userMarker.isInitialized) {
                userMarker.position = latLng
            }

        }
    }

    private fun handleLocationException(exception: Exception?): Boolean {
        exception ?: return false

        Timber.e(exception, "handleLocationException()")

        when(exception) {
            is SecurityException -> checkLocationPermission(REQUEST_PERMISSION_LOCATION_START_UPDATE)
            is ResolvableApiException -> exception.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
        }

        return true
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkNotificationPermission(requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                requestCode
            )
            return false
        }
        return true
    }

    private fun checkLocationPermission(requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                requestCode
            )
            return false
        }
        return true
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED)
            return

        when(requestCode) {
            REQUEST_PERMISSION_LOCATION_START_UPDATE -> locationLiveData.startRequestLocation()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.maps_style))
        map.setInfoWindowAdapter(EndorInfoWindowAdapter(this))
        map.setOnInfoWindowClickListener { showPoiDetail(it.tag as Poi) }
    }

    private fun showPoiDetail(poi: Poi) {
        if(poi.detailUrl.isEmpty())
            return

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(poi.detailUrl))
        startActivity(intent)
    }
}

private fun addPoiToMapMarker(poi: Poi, map: GoogleMap): Marker {
    val options = MarkerOptions()
        .position(LatLng(poi.latitude, poi.longitude))
        .title(poi.title)
        .snippet(poi.description)

    if(poi.iconId > 0) {
        options.icon(BitmapDescriptorFactory.fromResource(poi.iconId))
    } else if(poi.iconColor != 0) {
        val hue = when(poi.iconColor) {
            Color.BLUE -> BitmapDescriptorFactory.HUE_AZURE
            Color.GREEN -> BitmapDescriptorFactory.HUE_GREEN
            Color.YELLOW -> BitmapDescriptorFactory.HUE_YELLOW
            Color.RED -> BitmapDescriptorFactory.HUE_RED
            else -> BitmapDescriptorFactory.HUE_RED
        }
        options.icon(BitmapDescriptorFactory.defaultMarker(hue))
    }

    val marker = map.addMarker(options)
    marker!!.tag = poi
    return marker
}