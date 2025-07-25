package dev.yubin.elastic.product.domain.event

import dev.yubin.elastic.product.domain.Product
import java.util.UUID

sealed class ProductEvent

data class ProductCreatedEvent(val product: Product) : ProductEvent()
data class ProductDeletedEvent(val productId: String) : ProductEvent()