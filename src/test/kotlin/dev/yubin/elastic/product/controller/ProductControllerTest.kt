package dev.yubin.elastic.product.controller

import dev.yubin.elastic.product.service.ProductService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.mockito.kotlin.mock
import org.junit.jupiter.api.Test

@WebMvcTest(ProductController::class)
@Import(ProductControllerTest.MockConfig::class)
class ProductControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var productService: ProductService

    @TestConfiguration
    class MockConfig {
        @Bean
        fun productService(): ProductService = mock()
    }

    @Test
    fun dummyTest() {
        println("dummyTest")
    }
}
