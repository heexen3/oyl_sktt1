package cl.oyl.frutossecos.feature.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.ui.components.OylTextField
import cl.oyl.frutossecos.core.ui.components.PrimaryButton
import cl.oyl.frutossecos.core.ui.components.ProductCard
import cl.oyl.frutossecos.core.ui.components.SecondaryButton
import cl.oyl.frutossecos.data.repository.AuthRepository
import cl.oyl.frutossecos.data.repository.CartRepository
import cl.oyl.frutossecos.data.repository.ProductRepository
import cl.oyl.frutossecos.domain.model.Product
import cl.oyl.frutossecos.domain.model.Role
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ScanResult {
    data object Idle : ScanResult
    data class Found(val product: Product) : ScanResult
    data class NotFound(val raw: String) : ScanResult
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val products: ProductRepository,
    private val cart: CartRepository,
) : ViewModel() {
    val result = MutableStateFlow<ScanResult>(ScanResult.Idle)
    val added = MutableStateFlow<Int?>(null)
    val role: StateFlow<Role?> = auth.session.map { it?.role }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private var busy = false

    fun onCode(raw: String) {
        if (busy || result.value !is ScanResult.Idle) return
        busy = true
        viewModelScope.launch {
            val p = products.findByQr(raw)
            result.value = if (p != null) ScanResult.Found(p) else ScanResult.NotFound(raw.trim())
            busy = false
        }
    }

    fun reset() { result.value = ScanResult.Idle; added.value = null }

    fun addToCart(p: Product) = viewModelScope.launch {
        val s = auth.session.first() ?: return@launch
        added.value = cart.add(s.userId, p.id, 1)
    }
}

@Composable
fun ScannerScreen(
    onProduct: (Long) -> Unit,
    onCart: () -> Unit,
    onEdit: (Long) -> Unit,
    onAdjust: (Long) -> Unit,
    vm: ScannerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val result by vm.result.collectAsStateWithLifecycle()
    val role by vm.role.collectAsStateWithLifecycle()
    val added by vm.added.collectAsStateWithLifecycle()
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; denied = !it }
    var manual by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.scanner_hint), style = MaterialTheme.typography.bodyMedium)
        Box(
            Modifier.fillMaxWidth().height(300.dp).clip(MaterialTheme.shapes.large)
                .border(3.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center,
        ) {
            when {
                granted && result is ScanResult.Idle -> CameraPreview(onCode = vm::onCode)
                granted -> Text(stringResource(R.string.scanner_paused), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(if (denied) R.string.scanner_permission_denied else R.string.scanner_permission_rationale))
                    PrimaryButton(stringResource(R.string.scanner_permission_allow), { launcher.launch(Manifest.permission.CAMERA) })
                }
            }
            if (granted && result is ScanResult.Idle) Box(Modifier.size(200.dp).border(2.dp, Color.White.copy(alpha = 0.8f), MaterialTheme.shapes.medium))
        }
        // Ingreso manual: útil en emuladores sin cámara.
        OylTextField(manual, { manual = it }, stringResource(R.string.scanner_manual_label))
        SecondaryButton(stringResource(R.string.scanner_manual_search), { vm.reset(); vm.onCode(manual) }, enabled = manual.isNotBlank())

        when (val r = result) {
            ScanResult.Idle -> Unit
            is ScanResult.NotFound -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.scanner_not_found, r.raw), color = MaterialTheme.colorScheme.error)
                    SecondaryButton(stringResource(R.string.scanner_again), vm::reset)
                }
            }
            is ScanResult.Found -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.scanner_found), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ProductCard(r.product, onClick = { onProduct(r.product.id) })
                if (role == Role.ADMINISTRADOR) {
                    PrimaryButton(stringResource(R.string.action_adjust_inventory), { onAdjust(r.product.id) })
                    SecondaryButton(stringResource(R.string.action_edit_product), { onEdit(r.product.id) })
                } else {
                    added?.let { Text(stringResource(R.string.scanner_in_cart, it), color = MaterialTheme.colorScheme.primary) }
                    PrimaryButton(stringResource(R.string.action_add_to_cart), { vm.addToCart(r.product) }, enabled = !r.product.isOutOfStock)
                    if (added != null) SecondaryButton(stringResource(R.string.action_go_to_cart), onCart)
                }
                SecondaryButton(stringResource(R.string.scanner_again), vm::reset)
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onCode: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()) }
    val previewView = remember { PreviewView(context) }
    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null) { proxy.close(); return@setAnalyzer }
                scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                    .addOnSuccessListener { codes -> codes.firstOrNull()?.rawValue?.let(onCode) }
                    .addOnCompleteListener { proxy.close() }
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) {
                Log.w("Scanner", "No se pudo iniciar la cámara", e)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            executor.shutdown()
        }
    }
    AndroidView({ previewView }, Modifier.fillMaxSize())
}
