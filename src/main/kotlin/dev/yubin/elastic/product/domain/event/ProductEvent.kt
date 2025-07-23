package dev.yubin.elastic.product.domain.event

import dev.yubin.elastic.product.domain.Product

sealed class ProductEvent

data class ProductCreatedEvent(val product: Product) : ProductEvent()
data class ProductDeletedEvent(val productId: Long) : ProductEvent()