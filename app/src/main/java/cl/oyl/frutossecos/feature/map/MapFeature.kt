package cl.oyl.frutossecos.feature.map

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.ui.components.PrimaryButton
import cl.oyl.frutossecos.data.repository.StoreRepository
import cl.oyl.frutossecos.domain.logic.StoreHours
import cl.oyl.frutossecos.domain.model.Store
import cl.oyl.frutossecos.domain.model.StoreType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@HiltViewModel
class MapViewModel @Inject constructor(stores: StoreRepository) : ViewModel() {
    val stores: StateFlow<List<Store>> = stores.observeStores().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selected = MutableStateFlow<Store?>(null)
}

private val SANTIAGO = GeoPoint(-33.4489, -70.6693)

@Composable
fun MapScreen(vm: MapViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val stores by vm.stores.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    fun hasLocation() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    var locationGranted by remember { mutableStateOf(hasLocation()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r -> locationGranted = r.values.any { it } }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.0)
            controller.setCenter(SANTIAGO)
        }
    }
    val locationOverlay = remember { MyLocationNewOverlay(GpsMyLocationProvider(context), mapView) }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); locationOverlay.disableMyLocation(); mapView.onDetach() }
    }
    LaunchedEffect(locationGranted) {
        if (locationGranted) {
            locationOverlay.enableMyLocation()
            if (!mapView.overlays.contains(locationOverlay)) mapView.overlays.add(locationOverlay)
            mapView.invalidate()
        }
    }
    LaunchedEffect(stores) {
        mapView.overlays.removeAll { it is Marker }
        stores.forEach { st ->
            mapView.overlays.add(Marker(mapView).apply {
                position = GeoPoint(st.lat, st.lng)
                title = st.name
                snippet = st.address
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ -> vm.selected.value = st; true }
            })
        }
        if (vm.selected.value == null) vm.selected.value = stores.firstOrNull()
        mapView.invalidate()
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView({ mapView }, Modifier.fillMaxSize())
        SmallFloatingActionButton(
            onClick = {
                if (!locationGranted) launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                else locationOverlay.myLocation?.let { mapView.controller.animateTo(it) }
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        ) { Icon(Icons.Filled.MyLocation, stringResource(R.string.map_my_location)) }
        selected?.let { st ->
            StoreCard(st, Modifier.align(Alignment.BottomCenter).padding(12.dp)) {
                val uri = Uri.parse("geo:${st.lat},${st.lng}?q=${st.lat},${st.lng}(${Uri.encode(st.name)})")
                try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (e: ActivityNotFoundException) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/directions?to=${st.lat},${st.lng}")))
                }
            }
            LaunchedEffect(st.id) { mapView.controller.animateTo(GeoPoint(st.lat, st.lng)) }
        }
    }
}

@Composable
private fun StoreCard(st: Store, modifier: Modifier, onDirections: () -> Unit) {
    val open = remember(st.hours) { StoreHours.isOpen(st.hours, LocalDateTime.now()) }
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(6.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(st.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AssistChip(onClick = {}, label = { Text(stringResource(if (st.type == StoreType.TIENDA) R.string.map_type_store else R.string.map_type_pickup)) })
            }
            Text(st.address, style = MaterialTheme.typography.bodyMedium)
            Text(st.hours, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (open != null) {
                Text(stringResource(if (open) R.string.map_open else R.string.map_closed),
                    color = if (open) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
            PrimaryButton(stringResource(R.string.map_directions), onDirections)
        }
    }
}
