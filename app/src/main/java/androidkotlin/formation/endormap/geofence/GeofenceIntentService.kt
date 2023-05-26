package androidkotlin.formation.endormap.geofence

import android.annotation.SuppressLint
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidkotlin.formation.endormap.App
import androidkotlin.formation.endormap.R
import androidkotlin.formation.endormap.map.MapActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import timber.log.Timber

private const val NOTIFICATION_ID_MORDOR = 0

class GeofenceIntentService: IntentService("EndorGeofenceIntentService") {

    override fun onHandleIntent(intent: Intent?) {
        intent ?: return
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        geofencingEvent ?: return
        if(geofencingEvent.hasError()) {
            Timber.e("Error in Geofence Intent ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            Timber.e("Unhandler geofencing transition: $geofenceTransition")
        }

        if(geofencingEvent.triggeringGeofences == null) {
            Timber.w("Empty triggering geofences, nothing to do")
            return
        }

        for(triggeringGeofence in geofencingEvent.triggeringGeofences!!) {
            if(triggeringGeofence.requestId == GEOFENCE_ID_MORDOR) {
                sendMordorNotification(geofenceTransition)
            }
        }

    }

    @SuppressLint("MissingPermission")
    private fun sendMordorNotification(transitionType: Int) {
        val title: String
        val text: String
        val drawable: Drawable

        when(transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                title = "You entered the Mordor !"
                text = "Be careful... Sauron is always watching..."
                drawable = ContextCompat.getDrawable(this, R.drawable.sauroneye)!!
            }
            else -> {
                title = "You left the Mordor"
                text = "You can breath now... But where is the One Ring?"
                drawable = ContextCompat.getDrawable(this, R.drawable.mordorgate)!!
            }
        }

        val bitmap = (drawable as BitmapDrawable).bitmap

        val intent = Intent(this, MapActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val builder = NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setLargeIcon(bitmap)
            .setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap)
                .bigLargeIcon(null))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_MORDOR, builder.build())

    }

}