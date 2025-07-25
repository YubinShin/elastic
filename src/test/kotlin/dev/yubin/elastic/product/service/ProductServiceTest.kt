package dev.yubin.elastic.product.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.elasticsearch.core.ElasticsearchOperations

class ProductServiceTest {

    private lateinit var productService: ProductService
    private lateinit var elasticsearchOperations: ElasticsearchOperations

    @BeforeEach
    fun setUp() {
        elasticsearchOperations = mock()
        productService = ProductService(elasticsearchOperations)
    }

    @Test
    fun dummyTest() {

    }
}
