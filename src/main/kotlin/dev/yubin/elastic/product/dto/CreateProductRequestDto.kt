package dev.yubin.elastic.product.dto

data class CreateProductRequestDto(
    val name: String,
    val description: String,
    val price: Int,
    val rating: Double,
    val category: String
)