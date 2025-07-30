package dev.yubin.elastic.product.service

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery
import co.elastic.clients.elasticsearch._types.query_dsl.NumberRangeQuery
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery
import dev.yubin.elastic.product.domain.Product
import dev.yubin.elastic.product.domain.ProductDocument
import dev.yubin.elastic.product.domain.event.ProductCreatedEvent
import dev.yubin.elastic.product.domain.event.ProductDeletedEvent
import dev.yubin.elastic.product.dto.CreateProductRequestDto
import dev.yubin.elastic.product.dto.ProductSearchResultDto
import dev.yubin.elastic.product.repository.ProductRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.HighlightQuery
import org.springframework.data.elasticsearch.core.query.highlight.Highlight
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters

@Service
class ProductServiceImpl(
    private val productRepository: ProductRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val elasticsearchOperations: ElasticsearchOperations
) : ProductService {
    @Transactional
    override fun createProduct(dto: CreateProductRequestDto): Product {
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
    override fun createProducts(products: List<CreateProductRequestDto>): List<Product> {
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
    override fun deleteProduct(id: String) {
        productRepository.deleteById(id)
        eventPublisher.publishEvent(ProductDeletedEvent(id))
    }

    override fun getProducts(page: Int, size: Int): MutableList<Product?> {
        val pageable = PageRequest.of(page - 1, size)
        return productRepository.findAll(pageable).content
    }

    override fun getSuggestions(rawQuery: String): List<String> {
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
        return hits.searchHits.map { it.content.name }
    }


    override fun searchProducts(
        query: String,
        category: String?,
        minPrice: Double?,
        maxPrice: Double?,
        page: Int?,
        size: Int?
    ): List<ProductSearchResultDto> {

        val resolvedMinPrice = minPrice ?: 0.0
        val resolvedMaxPrice = maxPrice ?: 1_000_000_000.0
        val resolvedPage = (page ?: 1).coerceAtLeast(1) // 1페이지 미만 방지
        val resolvedSize = (size ?: 5).coerceIn(1, 100)  // 너무 큰 페이지 방지

        // multi_match 쿼리
        val multiMatchQuery: Query = MultiMatchQuery.of {
            it.query(query)
                .fields("name^3", "description^1", "category^2")
                .fuzziness("AUTO")
        }._toQuery()

        // filter 쿼리 조립
        val filters = mutableListOf<Query>()

        if (!category.isNullOrBlank()) {
            val categoryFilter = TermQuery.of {
                it.field("category.raw").value(category)
            }._toQuery()
            filters.add(categoryFilter)
        }

        val priceRangeFilter = NumberRangeQuery.of {
            it.field("price")
                .gte(resolvedMinPrice)
                .lte(resolvedMaxPrice)
        }._toRangeQuery()._toQuery()
        filters.add(priceRangeFilter)

        // rating > 4.0
        val ratingShould = NumberRangeQuery.of {
            it.field("rating").gt(4.0)
        }._toRangeQuery()._toQuery()

        // bool query 조합
        val boolQuery = BoolQuery.of {
            it.must(multiMatchQuery)
                .filter(filters)
                .should(ratingShould)
        }._toQuery()

        // Highlight 설정
        val highlightParams = HighlightParameters.builder()
            .withPreTags("<b>")
            .withPostTags("</b>")
            .build()

        val highlight = Highlight(highlightParams, listOf(HighlightField("name")))
        val highlightQuery = HighlightQuery(highlight, ProductDocument::class.java)

        // NativeQuery 조립
        val nativeQuery: NativeQuery = NativeQuery.builder()
            .withQuery(boolQuery)
            .withHighlightQuery(highlightQuery)
            .withPageable(PageRequest.of(resolvedPage - 1, resolvedSize))
            .build()

        // 검색 실행
        val searchHits = elasticsearchOperations.search(nativeQuery, ProductDocument::class.java)

        return searchHits.searchHits.map { hit ->
            val product = hit.content
            val highlightedName = hit.highlightFields["name"]?.firstOrNull()

            ProductSearchResultDto(
                id = product.id,
                name = product.name,
                highlightedName = highlightedName,
                description = product.description,
                price = product.price,
                rating = product.rating,
                category = product.category
            )
        }

    }
}