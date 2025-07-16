package dev.yubin.elastic.product

import dev.yubin.elastic.product.domain.Product
import dev.yubin.elastic.product.dto.CreateProductRequestDto
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ProductService(
    private val productRepository: ProductRepository
) {

    fun getProducts(page: Int, size: Int): MutableList<Product?> {
        val pageable = PageRequest.of(page - 1, size)
        return productRepository.findAll(pageable).content
    }

    fun createProduct(dto: CreateProductRequestDto): Product {
        val product = Product(
            name = dto.name,
            description = dto.description,
            price = dto.price,
            rating = dto.rating,
            category = dto.category
        )
        return productRepository.save(product)
    }

    fun deleteProduct(id: Long) {
        productRepository.deleteById(id)
    }
}