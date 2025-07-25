package dev.yubin.elastic.product.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Min

data class CreateProductRequestDto(
    @field:NotBlank val name: String,
    @field:NotBlank val description: String,
    @field:Min(1) val price: Int,
    @field:DecimalMin("0.0") val rating: Double,
    @field:NotBlank val category: String
)
