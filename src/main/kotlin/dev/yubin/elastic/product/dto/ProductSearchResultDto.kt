package dev.yubin.elastic.product.dto

data class ProductSearchResultDto(
    val id: String,
    val name: String,
    val highlightedName: String?,
    val description: String,
    val price: Int,
    val rating: Double,
    val category: String
)
