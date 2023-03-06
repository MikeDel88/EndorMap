package androidkotlin.formation.endormap

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import timber.log.Timber

private const val REQUEST_PERMISSION_LOCATION_START_UPDATE = 2
private const val REQUEST_CHECK_SETTINGS = 1

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var locationLiveData: LocationLiveData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapOptions = GoogleMapOptions()
            .mapType(GoogleMap.MAP_TYPE_NORMAL)
            .zoomControlsEnabled(true)
            .zoomGesturesEnabled(true)

        val mapFragment = SupportMapFragment.newInstance(mapOptions)
        mapFragment.getMapAsync(this)

        supportFragmentManager.beginTransaction()
            .replace(R.id.content, mapFragment)
            .commit()

        locationLiveData = LocationLiveData(this)
        locationLiveData.observe(this) {
            handleLocationData(it!!)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            REQUEST_CHECK_SETTINGS -> locationLiveData.startRequestLocation()
        }
    }

    private fun handleLocationData(locationData: LocationData) {
        if(handleLocationException(locationData.exception)) {
            return
        }

        //Timber.i("Last location from LIVEDATA ${locationData.location}")
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
    }
}