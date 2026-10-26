package cl.oyl.frutossecos.core.util

/** Formatea montos CLP enteros como "$12.470" (separador de miles con punto). */
object CurrencyFormatter {
    fun clp(amount: Long): String {
        val negative = amount < 0
        val digits = kotlin.math.abs(amount).toString()
        val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
        return (if (negative) "-$" else "$") + grouped
    }

    fun clp(amount: Int): String = clp(amount.toLong())
}
