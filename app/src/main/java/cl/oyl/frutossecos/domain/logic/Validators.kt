package cl.oyl.frutossecos.domain.logic

/** Errores de validación de formularios (se traducen a strings en la UI). */
enum class FieldError { REQUIRED, INVALID_EMAIL, INVALID_PHONE, PASSWORD_TOO_SHORT, PASSWORD_WEAK, ALIAS_INVALID, INVALID_NUMBER, NEGATIVE }

data class RegisterForm(
    val name: String = "",
    val alias: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val address: String = "",
)

/** Validaciones puras (sin Android) para que sean testeables. */
object Validators {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val ALIAS = Regex("^[A-Za-z0-9._-]{3,20}$")

    fun email(v: String): FieldError? = when {
        v.isBlank() -> FieldError.REQUIRED
        !EMAIL.matches(v.trim()) -> FieldError.INVALID_EMAIL
        else -> null
    }

    fun alias(v: String): FieldError? = when {
        v.isBlank() -> FieldError.REQUIRED
        !ALIAS.matches(v.trim()) -> FieldError.ALIAS_INVALID
        else -> null
    }

    /** Mínimo 8 caracteres con al menos una letra y un número. */
    fun password(v: String): FieldError? = when {
        v.isEmpty() -> FieldError.REQUIRED
        v.length < 8 -> FieldError.PASSWORD_TOO_SHORT
        v.none { it.isLetter() } || v.none { it.isDigit() } -> FieldError.PASSWORD_WEAK
        else -> null
    }

    /** Teléfono chileno: 9 dígitos (con o sin +56). */
    fun phone(v: String): FieldError? {
        if (v.isBlank()) return FieldError.REQUIRED
        val digits = v.filter { it.isDigit() }.removePrefix("56")
        return if (digits.length == 9) null else FieldError.INVALID_PHONE
    }

    fun required(v: String): FieldError? = if (v.isBlank()) FieldError.REQUIRED else null

    fun register(f: RegisterForm): Map<String, FieldError> = buildMap {
        required(f.name)?.let { put("name", it) }
        alias(f.alias)?.let { put("alias", it) }
        email(f.email)?.let { put("email", it) }
        password(f.password)?.let { put("password", it) }
        phone(f.phone)?.let { put("phone", it) }
    }

    fun nonNegativeLong(v: String): FieldError? = when {
        v.isBlank() -> FieldError.REQUIRED
        v.toLongOrNull() == null -> FieldError.INVALID_NUMBER
        v.toLong() < 0 -> FieldError.NEGATIVE
        else -> null
    }

    fun nonNegativeInt(v: String): FieldError? = when {
        v.isBlank() -> FieldError.REQUIRED
        v.toIntOrNull() == null -> FieldError.INVALID_NUMBER
        v.toInt() < 0 -> FieldError.NEGATIVE
        else -> null
    }
}
