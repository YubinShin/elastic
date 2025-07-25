package dev.yubin.elastic.product.service

import dev.yubin.elastic.product.domain.Product
import dev.yubin.elastic.product.domain.ProductDocument
import dev.yubin.elastic.product.dto.CreateProductRequestDto

interface ProductService {
    fun createProducts(products: List<CreateProductRequestDto>): List<Product>

    fun createProduct(dto: CreateProductRequestDto): Product

    fun deleteProduct(id: String)

    fun getSuggestions(rawQuery: String): List<String>

    fun getProducts(page: Int, size: Int): MutableList<Product?>

    fun searchProducts(
        query: String,
        category: String?,
        minPrice: Double?,
        maxPrice: Double?,
        page: Int?,
        size: Int?
    ): List<ProductDocument>
}
