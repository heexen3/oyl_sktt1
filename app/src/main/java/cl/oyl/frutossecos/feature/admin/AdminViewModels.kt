package cl.oyl.frutossecos.feature.admin

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.ui.components.UiMessage
import cl.oyl.frutossecos.data.repository.InventoryRepository
import cl.oyl.frutossecos.data.repository.ProductDraft
import cl.oyl.frutossecos.data.repository.ProductRepository
import cl.oyl.frutossecos.data.repository.StockException
import cl.oyl.frutossecos.domain.logic.FieldError
import cl.oyl.frutossecos.domain.logic.Validators
import cl.oyl.frutossecos.domain.model.Category
import cl.oyl.frutossecos.domain.model.MovementType
import cl.oyl.frutossecos.domain.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ProductFilter { TODOS, ACTIVOS, INACTIVOS, STOCK_BAJO }

data class ProductForm(
    val id: Long? = null,
    val name: String = "",
    val categoryId: Long? = null,
    val description: String = "",
    val price: String = "",
    val presentation: String = "",
    val weightGrams: String = "",
    val origin: String = "",
    val stock: String = "0",
    val minStock: String = "5",
    val active: Boolean = true,
    val imagePreview: String? = null,
    val newImagePath: String? = null,
    val errors: Map<String, FieldError> = emptyMap(),
    val saving: Boolean = false,
)

data class AdminProductsUiState(
    val loading: Boolean = true,
    val query: String = "",
    val filter: ProductFilter = ProductFilter.TODOS,
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val form: ProductForm? = null,
)

@HiltViewModel
class AdminProductsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val products: ProductRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(ProductFilter.TODOS)
    private val form = MutableStateFlow<ProductForm?>(null)
    val message = MutableStateFlow<UiMessage?>(null)

    val state: StateFlow<AdminProductsUiState> = combine(products.observeAll(), products.observeCategories(), query, filter, form) { all, cats, q, f, fm ->
        val list = all.filter { p ->
            (q.isBlank() || p.name.contains(q.trim(), ignoreCase = true)) && when (f) {
                ProductFilter.TODOS -> true
                ProductFilter.ACTIVOS -> p.active
                ProductFilter.INACTIVOS -> !p.active
                ProductFilter.STOCK_BAJO -> p.stock <= p.minStock
            }
        }
        AdminProductsUiState(false, q, f, list, cats, fm)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminProductsUiState())

    init {
        savedStateHandle.get<String>("id")?.toLongOrNull()?.takeIf { it > 0 }?.let { id ->
            viewModelScope.launch { products.observeProduct(id).first()?.let { edit(it) } }
        }
    }

    fun onQuery(v: String) { query.value = v }
    fun onFilter(f: ProductFilter) { filter.value = f }
    fun create() { form.value = ProductForm(categoryId = state.value.categories.firstOrNull()?.id) }
    fun edit(p: Product) {
        form.value = ProductForm(
            p.id, p.name, p.categoryId, p.description, p.price.toString(), p.presentation, p.weightGrams.toString(),
            p.origin, p.stock.toString(), p.minStock.toString(), p.active, p.imageUrl,
        )
    }
    fun dismiss() { form.value = null }
    fun update(transform: (ProductForm) -> ProductForm) { form.value = form.value?.let(transform) }

    fun toggleActive(p: Product) = viewModelScope.launch {
        products.setActive(p.id, !p.active)
        message.value = UiMessage(if (p.active) R.string.admin_product_deactivated else R.string.admin_product_activated, p.name)
    }

    /** Copia la imagen elegida al almacenamiento interno de la app (sobrevive a permisos temporales del selector). */
    fun onImagePicked(uri: Uri) = viewModelScope.launch {
        val path = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.filesDir, "products").apply { mkdirs() }
                val file = File(dir, "${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                file.absolutePath
            }.getOrNull()
        }
        if (path == null) message.value = UiMessage(R.string.admin_image_error)
        else update { it.copy(imagePreview = path, newImagePath = path) }
    }

    fun save() {
        val f = form.value ?: return
        val errors = buildMap {
            Validators.required(f.name)?.let { put("name", it) }
            if (f.categoryId == null) put("category", FieldError.REQUIRED)
            Validators.nonNegativeLong(f.price)?.let { put("price", it) }
            Validators.required(f.presentation)?.let { put("presentation", it) }
            Validators.nonNegativeInt(f.weightGrams)?.let { put("weight", it) }
            Validators.nonNegativeInt(f.stock)?.let { put("stock", it) }
            Validators.nonNegativeInt(f.minStock)?.let { put("minStock", it) }
        }
        if (errors.isNotEmpty()) { form.value = f.copy(errors = errors); return }
        form.value = f.copy(errors = emptyMap(), saving = true)
        viewModelScope.launch {
            products.save(
                ProductDraft(
                    id = f.id, name = f.name, categoryId = f.categoryId!!, description = f.description,
                    price = f.price.trim().toLong(), presentation = f.presentation, weightGrams = f.weightGrams.trim().toInt(),
                    origin = f.origin.ifBlank { "Chile" }, imagePath = f.newImagePath,
                    stock = f.stock.trim().toInt(), minStock = f.minStock.trim().toInt(), active = f.active,
                )
            )
            form.value = null
            message.value = UiMessage(R.string.admin_product_saved, f.name.trim())
        }
    }
}

data class InventoryUiState(
    val loading: Boolean = true,
    val query: String = "",
    val products: List<Product> = emptyList(),
    val pendingSync: Int = 0,
    val adjusting: Product? = null,
)

@HiltViewModel
class AdminInventoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    products: ProductRepository,
    private val inventory: InventoryRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val adjustingId = MutableStateFlow(savedStateHandle.get<String>("id")?.toLongOrNull()?.takeIf { it > 0 })
    val message = MutableStateFlow<UiMessage?>(null)

    val state: StateFlow<InventoryUiState> = combine(products.observeAll(), inventory.observePendingMovements(), query, adjustingId) { all, pending, q, adj ->
        val list = all.filter { q.isBlank() || it.name.contains(q.trim(), ignoreCase = true) }
            .sortedWith(compareByDescending<Product> { it.stock <= it.minStock }.thenBy { it.name })
        InventoryUiState(false, q, list, pending, all.firstOrNull { it.id == adj })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    fun onQuery(v: String) { query.value = v }
    fun open(p: Product) { adjustingId.value = p.id }
    fun dismiss() { adjustingId.value = null }

    fun adjust(p: Product, type: MovementType, qtyText: String): Boolean {
        val qty = qtyText.trim().toIntOrNull()
        if (qty == null || qty <= 0) { message.value = UiMessage(R.string.inventory_invalid_qty); return false }
        if (type == MovementType.SALIDA && qty > p.stock) { message.value = UiMessage(R.string.inventory_not_enough, p.stock.toString()); return false }
        viewModelScope.launch {
            try {
                val stock = inventory.adjust(p.id, type, qty)
                adjustingId.value = null
                message.value = UiMessage(R.string.inventory_adjusted, "${p.name}: $stock")
            } catch (e: StockException) {
                message.value = UiMessage(R.string.inventory_not_enough, p.stock.toString())
            }
        }
        return true
    }
}
