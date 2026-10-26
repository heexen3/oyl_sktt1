package cl.oyl.backend.service

import org.springframework.http.HttpStatus

data class StockIssue(val productId: Long, val name: String, val requested: Int, val available: Int)

open class ApiException(val status: HttpStatus, val code: String, message: String, val details: Any? = null) : RuntimeException(message)

class NotFoundException(what: String) : ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "$what no encontrado")
class StockConflictException(issues: List<StockIssue>) :
    ApiException(HttpStatus.CONFLICT, "STOCK_INSUFICIENTE", "No hay stock suficiente para algunos productos", issues)
class BadRequestException(code: String, message: String) : ApiException(HttpStatus.BAD_REQUEST, code, message)
class ForbiddenException : ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "No tienes permiso para este recurso")
