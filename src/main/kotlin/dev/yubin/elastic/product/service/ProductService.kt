package dev.yubin.elastic.product.service

import dev.yubin.elastic.product.domain.Product
import dev.yubin.elastic.product.domain.event.ProductCreatedEvent
import dev.yubin.elastic.product.domain.event.ProductDeletedEvent
import dev.yubin.elastic.product.dto.CreateProductRequestDto
import dev.yubin.elastic.product.repository.ProductRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val eventPublisher: ApplicationEventPublisher

) {

    fun getProducts(page: Int, size: Int): MutableList<Product?> {
        val pageable = PageRequest.of(page - 1, size)
        return productRepository.findAll(pageable).content
    }

    @Transactional
    fun createProduct(dto: CreateProductRequestDto): Product {
        val product = Product(
            name = dto.name,
            description = dto.description,
            price = dto.price,
            rating = dto.rating,
            category = dto.category
        )
        val saved = productRepository.save(product)
        eventPublisher.publishEvent(ProductCreatedEvent(saved))
        return productRepository.save(product)
    }

    @Transactional
    fun deleteProduct(id: Long) {
        productRepository.deleteById(id)
        eventPublisher.publishEvent(ProductDeletedEvent(id))
    }
}