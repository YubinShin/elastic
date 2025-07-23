package dev.yubin.elastic.product.service

import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import dev.yubin.elastic.product.domain.Product
import dev.yubin.elastic.product.domain.ProductDocument
import dev.yubin.elastic.product.domain.event.ProductCreatedEvent
import dev.yubin.elastic.product.domain.event.ProductDeletedEvent
import dev.yubin.elastic.product.dto.CreateProductRequestDto
import dev.yubin.elastic.product.repository.ProductRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val elasticsearchOperations: ElasticsearchOperations
) {

    fun getProducts(page: Int, size: Int): MutableList<Product?> {
        val pageable = PageRequest.of(page - 1, size)
        return productRepository.findAll(pageable).content
    }

    fun getSuggestions(rawQuery: String): List<String> {
        // Elasticsearch Java client DSL의 MultiMatchQuery 생성
        val mmq = MultiMatchQuery.of {
            it.query(rawQuery)
                .type(TextQueryType.BoolPrefix)
                .fields(
                    "name.auto_complete",
                    "name.auto_complete._2gram",
                    "name.auto_complete._3gram"
                )
        }

        // Spring Data Elasticsearch의 NativeQuery DSL로 래핑
        val nativeQuery = NativeQuery.builder()
            .withQuery(Query.of { qb -> qb.multiMatch(mmq) })
            .withPageable(PageRequest.of(0, 5))
            .build()

        // search 수행
        val hits = elasticsearchOperations.search(nativeQuery, ProductDocument::class.java)

        // 결과에서 name 필드만 추출
        return hits.map { it.content.name }.toList()
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
        return saved
    }

    @Transactional
    fun createProducts(products: List<CreateProductRequestDto>): List<Product> {
        val entities = products.map { dto ->
            Product(
                name = dto.name,
                description = dto.description,
                price = dto.price,
                rating = dto.rating,
                category = dto.category
            )
        }

        val saved = productRepository.saveAll(entities)

        // 이벤트는 개별 발행 (이벤트 기반 ES 연동 유지)
        saved.forEach { eventPublisher.publishEvent(ProductCreatedEvent(it)) }

        return saved
    }

    @Transactional
    fun deleteProduct(id: Long) {
        productRepository.deleteById(id)
        eventPublisher.publishEvent(ProductDeletedEvent(id))
    }
}