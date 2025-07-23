package dev.yubin.elastic.product.domain.event

import dev.yubin.elastic.product.domain.ProductDocument
import dev.yubin.elastic.product.repository.ProductDocumentRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.event.TransactionPhase

@Component
class ProductEventHandler(
    private val productDocumentRepository: ProductDocumentRepository
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleProductCreatedEvent(event: ProductCreatedEvent) {
        val product = event.product

        val document = ProductDocument(
            id = product.id.toString(),
            name = product.name,
            description = product.description,
            price = product.price,
            rating = product.rating,
            category = product.category
        )

        productDocumentRepository.save(document)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleProductDeletedEvent(event: ProductDeletedEvent) {
        productDocumentRepository.deleteById(event.productId.toString())
    }
}
