package dev.yubin.elastic.global.exception

import io.swagger.v3.oas.annotations.Hidden
import jakarta.validation.ConstraintViolationException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice()
@Hidden
class GlobalExceptionHandler {

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(ex: CustomException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(code = ex.code, message = ex.message)
        return ResponseEntity.internalServerError().body(response)
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(code = "INTERNAL_ERROR", message = ex.message ?: "내부 서버 오류")
        return ResponseEntity.internalServerError().body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(code = "UNKNOWN", message = "알 수 없는 오류")
        return ResponseEntity.internalServerError().body(response)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        ex.printStackTrace()

        val errorMessages = ex.bindingResult
            .fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }

        val response = ErrorResponse(
            code = "INVALID_REQUEST",
            message = errorMessages
        )
        return ResponseEntity.badRequest().body(response)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        ex.printStackTrace()

        val message = ex.constraintViolations.joinToString(", ") {
            "${it.propertyPath}: ${it.message}"
        }

        val response = ErrorResponse(
            code = "INVALID_REQUEST",
            message = message
        )

        return ResponseEntity.badRequest().body(response)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableJson(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        ex.printStackTrace()
        val response = ErrorResponse(
            code = "INVALID_JSON",
            message = "요청 바디가 잘못되었습니다."
        )
        return ResponseEntity.badRequest().body(response)
    }

}
