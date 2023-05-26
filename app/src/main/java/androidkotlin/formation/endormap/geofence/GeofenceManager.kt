package androidkotlin.formation.endormap.geofence

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidkotlin.formation.endormap.poi.Poi
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import timber.log.Timber

const val GEOFENCE_ID_MORDOR = "Mordor"

class GeofenceManager(context: Context) {

    private val appContext = context.applicationContext

    private val geofencingClient = LocationServices.getGeofencingClient(appContext)
    private val geofenceList = mutableListOf<Geofence>()

    @SuppressLint("MissingPermission")
    fun createGeofence(poi: Poi, radiusMeter: Float, requestId: String) {
        Timber.d("Creating geofence at coordinates ${poi.latitude}, ${poi.longitude}")

        geofenceList.add(
            Geofence.Builder()
                .setRequestId(requestId)
                .setExpirationDuration(10 * 60 * 1000)
                .setCircularRegion(poi.latitude, poi.longitude, radiusMeter)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        )

        val task = geofencingClient.addGeofences(getGeofencingRequest(), geofencePendingIntent)
        task.addOnSuccessListener {
            Timber.i("Geofence added")
        }
        task.addOnFailureListener { exception ->
            Timber.e(exception, "Cannot add Geofence")
        }
    }

    fun removeAllGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)
        geofenceList.clear()
    }

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(appContext, GeofenceIntentService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getService(appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE) //FIXME Error on API 31 FLAG.
        } else {
            PendingIntent.getService(appContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }


    }

}