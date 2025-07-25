package dev.yubin.elastic.product.service

import dev.yubin.elastic.product.domain.Product
import dev.yubin.elastic.product.domain.ProductDocument
import dev.yubin.elastic.product.domain.event.ProductCreatedEvent
import dev.yubin.elastic.product.domain.event.ProductDeletedEvent
import dev.yubin.elastic.product.dto.CreateProductRequestDto
import dev.yubin.elastic.product.repository.ProductRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import org.mockito.Mock
import org.mockito.kotlin.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.data.elasticsearch.core.query.Query
import org.mockito.ArgumentMatchers.any as anyObj

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores::class)
class ProductServiceTest {
    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var elasticsearchOperations: ElasticsearchOperations
    private lateinit var productService: ProductServiceImpl

    @BeforeEach
    fun setUp() {
        productRepository = mock()
        eventPublisher = mock()
        elasticsearchOperations = mock()

        productService = ProductServiceImpl(
            productRepository,
            eventPublisher,
            elasticsearchOperations
        )
    }

    companion object {
        fun dummyProduct(
            id: String = "11111111-1111-1111-1111-111111111111",
            name: String = "돌김",
            description: String = "맛있는 김",
            price: Int = 1000,
            rating: Double = 4.5,
            category: String = "식품",
        ): ProductDocument {
            return ProductDocument(id, name, description, price, rating, category)
        }

        fun hitOf(
            doc: ProductDocument,
            highlights: Map<String, List<String>> = emptyMap()
        ): SearchHit<ProductDocument> {
            return mock {
                on { content } doReturn doc
                on { highlightFields } doReturn highlights
            }
        }

        fun dummyCreateDto(
            name: String = "돌김",
            description: String = "맛있는 김",
            price: Int = 1000,
            rating: Double = 4.5,
            category: String = "식품"
        ) = CreateProductRequestDto(name, description, price, rating, category)

        fun createDto(name: String, description: String, price: Int, rating: Double, category: String) =
            CreateProductRequestDto(name, description, price, rating, category)

        fun toProduct(dto: CreateProductRequestDto, id: String): Product {
            return Product(
                id = id,
                name = dto.name,
                description = dto.description,
                price = dto.price,
                rating = dto.rating,
                category = dto.category
            )
        }
    }

    @Nested
    @DisplayName("searchProduct")
    inner class SearchProductTest {

        @Test
        fun should_return_highlighted_product_when_only_query_is_provided() {
            // given
            val doc = dummyProduct(name = "돌김")
            val hit = hitOf(
                doc,
                highlights = mapOf("name" to listOf("<b>돌김</b>"))
            )

            val hits: SearchHits<ProductDocument> = mock {
                on { searchHits } doReturn listOf(hit)
            }

            whenever(elasticsearchOperations.search(any<Query>(), eq(ProductDocument::class.java)))
                .thenReturn(hits)

            // when
            val result = productService.searchProducts("돌김", null, null, null, null, null)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("<b>돌김</b>")
        }

        @Test
        fun should_return_name_without_highlight_when_highlight_missing() {
            val doc = dummyProduct(name = "돌김")

            val hit: SearchHit<ProductDocument> = mock {
                on { content } doReturn doc
                on { highlightFields } doReturn emptyMap()
            }

            val hits: SearchHits<ProductDocument> = mock {
                on { searchHits } doReturn listOf(hit)
            }

            whenever(elasticsearchOperations.search(any<Query>(), eq(ProductDocument::class.java)))
                .thenReturn(hits)

            val result = productService.searchProducts("돌김", null, null, null, null, null)

            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("돌김")
        }

        @Test
        fun should_return_products_within_price_range_only() {
            // given
            val doc = dummyProduct(name = "돌김", price = 5000)
            val hit = hitOf(doc, highlights = mapOf("name" to listOf("<b>돌김</b>")))

            val hits: SearchHits<ProductDocument> = mock {
                on { searchHits } doReturn listOf(hit)
            }

            whenever(elasticsearchOperations.search(any<Query>(), eq(ProductDocument::class.java))).thenReturn(hits)

            // when
            val result = productService.searchProducts("돌김", null, 1000.0, 10000.0, null, null)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("<b>돌김</b>")
            assertThat(result[0].price).isBetween(1000, 10000)
        }

        @Test
        fun should_use_default_pagination_when_page_and_size_null() {
            // given
            val doc = dummyProduct(name = "돌김")
            val hit = hitOf(doc, highlights = mapOf("name" to listOf("<b>돌김</b>")))

            val hits: SearchHits<ProductDocument> = mock {
                on { searchHits } doReturn listOf(hit)
            }

            val queryCaptor = argumentCaptor<Query>()
            whenever(elasticsearchOperations.search(queryCaptor.capture(), eq(ProductDocument::class.java))).thenReturn(
                hits
            )

            // when
            productService.searchProducts("돌김", null, null, null, null, null)

            // then
            val nativeQuery = queryCaptor.firstValue as NativeQuery
            val pageable = nativeQuery.pageable

            assertThat(pageable.pageNumber).isEqualTo(0) // page=1 이면 index 0
            assertThat(pageable.pageSize).isEqualTo(5)   // 기본값
        }
    }

    @Nested
    @DisplayName("suggestProduct")
    inner class SuggestProductTest {
//        /** 검색어 자동 완성 시 검색 결과가 존재 하는 경우 */
//        @Test
//        fun should_return_suggestions_when_matches_exist() {
//            // given
//            val doc1 = dummyProduct(name = "돌김 도시락")
//            val doc2 = dummyProduct(name = "돌김밥 세트")
//            val hit1 = hitOf(doc1)
//            val hit2 = hitOf(doc2)
//
////            val hits: SearchHits<ProductDocument> = mock {
////                on { map(any<(SearchHit<ProductDocument>) -> String>()) } doAnswer { invocation ->
////                    @Suppress("UNCHECKED_CAST")
////                    val mapper = invocation.arguments[0] as (SearchHit<ProductDocument>) -> String
////                    Streamable.of(listOf(
////                        mapper(hit1),
////                        mapper(hit2)
////                    ))
////                }
////            }
//            val hits: SearchHits<ProductDocument> = mock {
//                on { searchHits } doReturn listOf(hit1, hit2)
//            }
//            whenever(elasticsearchOperations.search(any<Query>(), eq(ProductDocument::class.java)))
//                .thenReturn(hits)
//
//            // when
//            val result = productService.getSuggestions("돌김")
//
//            // then
//            assertThat(result).containsExactly("돌김 도시락", "돌김밥 세트")
//        }
//
//        @Test
//        fun should_use_default_pagination_when_suggestions_requested() {
//            // given
//            val doc = dummyProduct(name = "돌김 도시락")
//            val hit = hitOf(doc)
//
//            val hits: SearchHits<ProductDocument> = mock {
//                on { map(any<(SearchHit<ProductDocument>) -> String>()) } doAnswer { invocation ->
//                    @Suppress("UNCHECKED_CAST")
//                    val mapper = invocation.arguments[0] as (SearchHit<ProductDocument>) -> String
//                    Streamable.of(listOf(mapper(hit)))
//                }
//            }
//
//
//            val queryCaptor = argumentCaptor<Query>()
//            whenever(
//                elasticsearchOperations.search(queryCaptor.capture(), eq(ProductDocument::class.java))
//            ).thenReturn(hits)
//
//            // when
//            productService.getSuggestions("돌김")
//
//            // then
//            val nativeQuery = queryCaptor.firstValue as NativeQuery
//            val pageable = nativeQuery.pageable
//
//            assertThat(pageable.pageNumber).isEqualTo(0)
//            assertThat(pageable.pageSize).isEqualTo(5)
//        }
    }

    @Nested
    @DisplayName("createProduct")
    inner class CreateProductTest {

        /** 단일 create 성공 */
        @Test
        fun should_save_product_and_publish_event_when_created() {
            // given
            val dto = createDto("돌김", "맛있는 김", 1000, 4.5, "식품")
            val savedEntity = toProduct(
                dto,
                "11111111-1111-1111-1111-111111111111"
            )

            whenever(productRepository.save(anyObj())).thenReturn(savedEntity)

            // when
            val result = productService.createProduct(dto)

            // then
            assertThat(result).isEqualTo(savedEntity)
            verify(productRepository).save(check {
                assertThat(it.name).isEqualTo("돌김")
            })
            verify(eventPublisher).publishEvent(isA<ProductCreatedEvent>())
        }

        /** 단일 create 실패 */
        @Test
        fun should_throw_exception_when_product_save_fails() {
            // given
            val dto = dummyCreateDto(name = "돌김")
            whenever(productRepository.save(anyObj())).thenThrow(RuntimeException("DB 오류"))

            // when & then
            assertThatThrownBy {
                productService.createProduct(dto)
            }.isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("DB 오류")

            verify(productRepository).save(any())
            verify(eventPublisher, never()).publishEvent(any())
        }

        @Test
        fun should_throw_exception_when_event_publishing_fails_on_createProduct() {
            // given
            val dto = dummyCreateDto()
            val saved = toProduct(dto, "11111111-1111-1111-1111-111111111111")

            whenever(productRepository.save(anyObj())).thenReturn(saved)

//            whenever(productRepository.save(any())).thenReturn(saved)
            doThrow(RuntimeException("이벤트 실패")).whenever(eventPublisher)
                .publishEvent(any<ProductCreatedEvent>())

            // when & then
            assertThatThrownBy {
                productService.createProduct(dto)
            }.isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("이벤트 실패")

            verify(productRepository).save(any())
            verify(eventPublisher).publishEvent(any<ProductCreatedEvent>())
        }
    }

    @Nested
    @DisplayName("createProducts")
    inner class CreateProductsTest {
        @Test
        fun should_save_multiple_products_and_publish_events() {
            // given
            val dtos = listOf(
                createDto("김A", "A", 1000, 4.2, "식품"),
                createDto("김B", "B", 2000, 4.8, "식품")
            )
            val savedEntities = listOf(
                toProduct(dtos[0], "11111111-1111-1111-1111-111111111111"),
                toProduct(dtos[1], "22222222-2222-2222-2222-222222222222")
            )


            whenever(productRepository.saveAll(any<Iterable<Product>>()))
                .thenReturn(savedEntities)

            // when
            val result = productService.createProducts(dtos)

            // then
            assertThat(result).hasSize(2)
            verify(productRepository).saveAll(any<Iterable<Product>>())

            val eventCaptor = argumentCaptor<Any>()
            verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture())

            val publishedNames = eventCaptor.allValues
                .filterIsInstance<ProductCreatedEvent>()
                .map { it.product.name }

            assertThat(publishedNames).containsExactly("김A", "김B")
        }

        @Test
        fun should_throw_exception_when_bulk_save_fails() {
            // given
            val dtos = listOf(
                createDto("돌김", "맛있는 김", 1000, 4.5, "식품")
            )

            whenever(productRepository.saveAll(any<Iterable<Product>>()))
                .thenThrow(RuntimeException("DB 일괄 저장 실패"))

            // when & then
            assertThatThrownBy {
                productService.createProducts(dtos)
            }.isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("DB 일괄 저장 실패")

            verify(productRepository).saveAll(any<Iterable<Product>>())
            verify(eventPublisher, never()).publishEvent(any())
        }

        /** bulk create 실패 */
        @Test
        fun should_throw_exception_when_event_publishing_fails() {
            // given
            val dtos = listOf(
                createDto("돌김", "맛있는 김", 1000, 4.5, "식품")
            )
            val entity = toProduct(
                dtos[0],
                "11111111-1111-1111-1111-111111111111",
            )

            whenever(productRepository.saveAll(any<Iterable<Product>>())).thenReturn(listOf(entity))
            doThrow(RuntimeException("이벤트 실패")).whenever(eventPublisher)
                .publishEvent(any<ProductCreatedEvent>())

            // when & then
            assertThatThrownBy {
                productService.createProducts(dtos)
            }.isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("이벤트 실패")

            verify(productRepository).saveAll(any<Iterable<Product>>())
            verify(eventPublisher).publishEvent(any<ProductCreatedEvent>())
        }
    }


    @Nested
    @DisplayName("deleteProduct")
    inner class DeleteProductTest {
        @Test
        fun should_delete_product_and_publish_event() {
            // given
            val productId = "11111111-1111-1111-1111-111111111111"

            // when
            productService.deleteProduct(productId)

            // then
            verify(productRepository).deleteById(productId)
            verify(eventPublisher).publishEvent(
                check<ProductDeletedEvent> {
                    assertThat(it.productId).isEqualTo(productId)
                }
            )
        }

        @Test
        fun should_not_publish_event_when_deletion_fails() {
            // given
            val productId = "11111111-1111-1111-1111-111111111111"
            whenever(productRepository.deleteById(productId)).thenThrow(RuntimeException("삭제 실패"))

            // when & then
            assertThatThrownBy {
                productService.deleteProduct(productId)
            }.isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("삭제 실패")

            verify(productRepository).deleteById(productId)
            verify(eventPublisher, never()).publishEvent(any())
        }
    }
}


