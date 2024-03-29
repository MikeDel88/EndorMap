package androidkotlin.formation.endormap.map

import androidkotlin.formation.endormap.poi.Poi
import androidkotlin.formation.endormap.poi.generatePois
import androidkotlin.formation.endormap.poi.generateUserPoi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import timber.log.Timber

sealed class MapUiState {
    object Loading : MapUiState()
    data class Error(val errorMessage: String) : MapUiState()
    data class PoiReady(
        val userPoi: Poi? = null,
        val pois: List<Poi>? = null
    ) : MapUiState()
}

class MapViewModel : ViewModel() {

    private val uiState = MutableLiveData<MapUiState>()
    fun getUiState(): LiveData<MapUiState> = uiState


    fun loadPois(latitude: Double, longitude: Double) {
        Timber.i("loadPois()")
        if(!(latitude in -90.0..90.0 && longitude in -180.0..180.0)) {
            uiState.value = MapUiState.Error("Invalid coordinates: lat=$latitude, long=$longitude")
            return
        }

        uiState.value = MapUiState.Loading
        uiState.value = MapUiState.PoiReady(
            userPoi = generateUserPoi(latitude, longitude),
            pois = generatePois(latitude, longitude)
        )
    }

}