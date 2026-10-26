package cl.oyl.backend.web

import cl.oyl.backend.domain.OrderStatus
import cl.oyl.backend.repo.StoreRepository
import cl.oyl.backend.security.CurrentUser
import cl.oyl.backend.service.ApiException
import cl.oyl.backend.service.AuthService
import cl.oyl.backend.service.BadRequestException
import cl.oyl.backend.service.InventoryService
import cl.oyl.backend.service.Line
import cl.oyl.backend.service.NotificationService
import cl.oyl.backend.service.OrderService
import cl.oyl.backend.service.ProductInput
import cl.oyl.backend.service.ProductService
import jakarta.validation.Valid
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val auth: AuthService) {
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody r: RegisterRequest) =
        auth.register(r.name, r.alias, r.email, r.password, r.phone, r.address, r.avatarId).let { AuthResponse(it.token, it.user.toDto()) }

    @PostMapping("/login")
    fun login(@Valid @RequestBody r: LoginRequest) = auth.login(r.login, r.password, r.role).let { AuthResponse(it.token, it.user.toDto()) }
}

@RestController
@RequestMapping("/api/v1/products")
class ProductController(private val products: ProductService, @Value("\${oyl.uploads-dir}") private val uploadsDir: String) {
    @GetMapping
    fun list(@RequestParam(required = false) category: Long?, @RequestParam(required = false) q: String?, @RequestParam(required = false) active: Boolean?) =
        products.search(category, q, active).map { it.toDto() }

    @GetMapping("/{id}") fun get(@PathVariable id: Long) = products.get(id).toDto()
    @GetMapping("/by-qr/{code}") fun byQr(@PathVariable code: String) = products.byQr(code).toDto()

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody r: ProductRequest) = products.create(r.toInput()).toDto()

    @PutMapping("/{id}") fun update(@PathVariable id: Long, @Valid @RequestBody r: ProductRequest) = products.update(id, r.toInput()).toDto()
    @PatchMapping("/{id}/active") fun active(@PathVariable id: Long, @RequestBody r: ActiveRequest) = products.setActive(id, r.active).toDto()

    @PostMapping("/{id}/image")
    fun image(@PathVariable id: Long, @RequestParam("file") file: MultipartFile): ProductDto {
        products.get(id)
        val type = file.contentType ?: ""
        if (!type.startsWith("image/")) throw BadRequestException("INVALID_IMAGE", "El archivo debe ser una imagen")
        val dir = Path.of(uploadsDir).toAbsolutePath().also { Files.createDirectories(it) }
        val name = "product-$id-${UUID.randomUUID()}.${if (type.endsWith("png")) "png" else "jpg"}"
        file.inputStream.use { Files.copy(it, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING) }
        return products.setImage(id, "/uploads/$name").toDto()
    }

    private fun ProductRequest.toInput() = ProductInput(name.trim(), categoryId, description.trim(), price, presentation.trim(), weightGrams, origin.trim(), stock, minStock, active, imageUrl)
}

@RestController
@RequestMapping("/api/v1")
class OrderController(private val orders: OrderService) {
    @PostMapping("/cart/validate")
    fun validate(@Valid @RequestBody r: CartValidateRequest) = orders.validate(r.items.map { Line(it.productId, it.quantity) })

    @PostMapping("/orders") @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody r: CreateOrderRequest) =
        orders.place(CurrentUser.get().id, r.items.map { Line(it.productId, it.quantity) }, r.deliveryMode).toDto()

    @GetMapping("/orders")
    fun list(@RequestParam(required = false) status: OrderStatus?) = orders.list(CurrentUser.get(), status).map { it.toDto() }

    @GetMapping("/orders/{id}") fun get(@PathVariable id: Long) = orders.get(CurrentUser.get(), id).toDto()

    @PatchMapping("/orders/{id}/status") fun status(@PathVariable id: Long, @RequestBody r: StatusRequest) = orders.changeStatus(id, r.status).toDto()
}

@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(private val inventory: InventoryService, private val products: ProductService) {
    @GetMapping fun list(@RequestParam(defaultValue = "false") lowStock: Boolean) = products.search(null, null, null, lowStock).map { it.toDto() }

    @PostMapping("/movements")
    fun movement(@Valid @RequestBody r: MovementRequest): ResponseEntity<MovementResponse> {
        val res = inventory.apply(r.localId, r.productId, r.type, r.quantity)
        val m = res.movement
        return ResponseEntity.status(if (res.duplicated) HttpStatus.OK else HttpStatus.CREATED)
            .body(MovementResponse(m.localId, m.productId, m.type, m.quantity, m.resultingStock, res.duplicated))
    }
}

@RestController
@RequestMapping("/api/v1")
class MiscController(private val stores: StoreRepository, private val notifications: NotificationService) {
    @GetMapping("/stores") fun stores() = stores.findAll().map { it.toDto() }
    @GetMapping("/notifications") fun notifications() = notifications.list(CurrentUser.get().id).map { it.toDto() }
    @PostMapping("/notifications/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun read(@PathVariable id: Long) = notifications.markRead(CurrentUser.get().id, id)
    @PostMapping("/devices/fcm-token") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun token(@Valid @RequestBody r: FcmTokenRequest) = notifications.registerToken(CurrentUser.get().id, r.token)
}

@RestControllerAdvice
class ErrorHandler {
    @ExceptionHandler(ApiException::class)
    fun api(e: ApiException) = ResponseEntity.status(e.status).body(ErrorResponse(e.code, e.message ?: "", e.details))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalid(e: MethodArgumentNotValidException) = ResponseEntity.badRequest().body(
        ErrorResponse("VALIDATION_ERROR", "Datos inválidos", e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "inválido") }))
}
