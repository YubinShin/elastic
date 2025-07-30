package dev.yubin.elastic.product.controller

import dev.yubin.elastic.global.exception.GlobalExceptionHandler
import java.util.UUID

import dev.yubin.elastic.product.domain.Product
import dev.yubin.elastic.product.domain.ProductDocument
import dev.yubin.elastic.product.dto.CreateProductRequestDto
import dev.yubin.elastic.product.dto.ProductSearchResultDto
import dev.yubin.elastic.product.service.ProductService

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(ProductController::class)
@Import(ProductControllerTest.MockConfig::class, GlobalExceptionHandler::class)
@DisplayName("ProductController 테스트")
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

    @Nested
    @DisplayName("GET /products")
    inner class GetProductsTest {
        @Test
        fun should_return_product_list_with_default_pagination() {
            // given
            val products = listOf(
                Product(UUID.randomUUID().toString(), "김A", "A", 1000, 4.2, "식품"),
                Product(UUID.randomUUID().toString(), "김B", "B", 2000, 4.8, "식품")
            )

            whenever(productService.getProducts(1, 10)).thenReturn(products.toMutableList())

            // when & then
            mockMvc.perform(get("/products"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("김A"))
                .andExpect(jsonPath("$[1].name").value("김B"))
        }
    }

    @Nested
    @DisplayName("GET /products/search")
    inner class SearchProductsTest {

        @Test
        fun should_return_search_results_when_query_only_provided() {
            // given
            val results = listOf(
                ProductSearchResultDto(
                    id = "11111111-1111-1111-1111-111111111111",
                    name = "김A",
                    highlightedName = "<b>김</b>A",
                    description = "A 상품입니다",
                    price = 1000,
                    rating = 4.2,
                    category = "식품"
                ),
                ProductSearchResultDto(
                    id = "11111111-1111-1111-1111-111111111110",
                    name = "김B",
                    highlightedName = "<b>김</b>B",
                    description = "B 상품입니다",
                    price = 2000,
                    rating = 4.5,
                    category = "식품"
                )
            )

            whenever(
                productService.searchProducts(
                    query = "김",
                    category = null,
                    minPrice = null,
                    maxPrice = null,
                    page = null,
                    size = null
                )
            ).thenReturn(results)

            // when & then
            mockMvc.perform(get("/products/search").param("query", "김"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("김A"))
                .andExpect(jsonPath("$[0].highlightedName").value("<b>김</b>A"))
                .andExpect(jsonPath("$[1].name").value("김B"))
                .andExpect(jsonPath("$[1].highlightedName").value("<b>김</b>B"))
        }

        @Test
        fun should_return_filtered_results_when_category_and_price_given() {
            // given
            val results = listOf(
                ProductSearchResultDto(
                    id = "11111111-1111-1111-1111-111111111100",
                    name = "김C",
                    highlightedName = "<b>김</b>C",
                    description = "C 상품입니다",
                    price = 1500,
                    rating = 4.5,
                    category = "간식"
                )
            )

            whenever(
                productService.searchProducts(
                    query = "김",
                    category = "간식",
                    minPrice = 1000.0,
                    maxPrice = 2000.0,
                    page = null,
                    size = null
                )
            ).thenReturn(results)

            // when & then
            mockMvc.perform(
                get("/products/search")
                    .param("query", "김")
                    .param("category", "간식")
                    .param("minPrice", "1000.0")
                    .param("maxPrice", "2000.0")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].name").value("김C"))
                .andExpect(jsonPath("$[0].highlightedName").value("<b>김</b>C"))
                .andExpect(jsonPath("$[0].category").value("간식"))
                .andExpect(jsonPath("$[0].price").value(1500))
        }
    }


    @Nested
    @DisplayName("GET /products/suggestions")
    inner class SuggestProductsTest {

        @Test
        fun should_return_suggestions_for_short_query() {
            // given
            val suggestions = listOf("김A", "김B", "김치젓갈", "김말이튀김", "김밥세트")

            whenever(productService.getSuggestions("김")).thenReturn(suggestions)

            // when & then
            mockMvc.perform(get("/products/suggestions").param("query", "김"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.size()").value(5))
                .andExpect(jsonPath("$[0]").value("김A"))
                .andExpect(jsonPath("$[1]").value("김B"))
                .andExpect(jsonPath("$[2]").value("김치젓갈"))
                .andExpect(jsonPath("$[3]").value("김말이튀김"))
                .andExpect(jsonPath("$[4]").value("김밥세트"))
        }
    }

    @Nested
    @DisplayName("POST /products")
    inner class CreateProductTest {

        @Test
        fun should_create_product_and_return_200() {
            // given
            val dto = CreateProductRequestDto("김라면", "매운맛", 1200, 4.5, "식품")
            val saved = Product(
                id = UUID.randomUUID().toString(),
                name = dto.name,
                description = dto.description,
                price = dto.price,
                rating = dto.rating,
                category = dto.category
            )

            whenever(productService.createProduct(dto)).thenReturn(saved)

            // when & then
            mockMvc.perform(
                post("/products")
                    .contentType("application/json")
                    .content(
                        """
                    {
                        "name": "${dto.name}",
                        "description": "${dto.description}",
                        "price": ${dto.price},
                        "rating": ${dto.rating},
                        "category": "${dto.category}"
                    }
                    """.trimIndent()
                    )
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value(dto.name))
                .andExpect(jsonPath("$.price").value(dto.price))
        }

        @Test
        fun should_return_400_when_request_body_is_invalid() {
            // name, price 누락된 잘못된 요청
            val invalidJson = """
            {
                "description": "맛있음",
                "rating": 4.2,
                "category": "식품"
            }
        """.trimIndent()

            mockMvc.perform(
                post("/products")
                    .contentType("application/json")
                    .content(invalidJson)
            )
                .andExpect(status().isBadRequest)
        }
    }


    @Nested
    @DisplayName("POST /products/bulk")
    inner class CreateProductsBulkTest {

        @Test
        fun should_create_multiple_products_and_return_list() {
            // given
            val dtos = listOf(
                CreateProductRequestDto("김A", "A", 1000, 4.2, "식품"),
                CreateProductRequestDto("김B", "B", 2000, 4.8, "식품")
            )

            val saved = listOf(
                Product(UUID.randomUUID().toString(), "김A", "A", 1000, 4.2, "식품"),
                Product(UUID.randomUUID().toString(), "김B", "B", 2000, 4.8, "식품")
            )

            whenever(productService.createProducts(dtos)).thenReturn(saved)

            // when & then
            mockMvc.perform(
                post("/products/bulk")
                    .contentType("application/json")
                    .content(
                        """
                    [
                        {
                            "name": "김A",
                            "description": "A",
                            "price": 1000,
                            "rating": 4.2,
                            "category": "식품"
                        },
                        {
                            "name": "김B",
                            "description": "B",
                            "price": 2000,
                            "rating": 4.8,
                            "category": "식품"
                        }
                    ]
                    """.trimIndent()
                    )
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("김A"))
                .andExpect(jsonPath("$[1].price").value(2000))
        }

        @Test
        fun should_return_400_when_any_product_is_invalid() {
            // invalid: 두 번째 dto는 name 없음, price < 0
            val invalidPayload = """
        [
            {
                "name": "김A",
                "description": "A",
                "price": 1000,
                "rating": 4.2,
                "category": "식품"
            },
            {
                "description": "B",
                "price": -500,
                "rating": 4.8,
                "category": "식품"
            }
        ]
    """.trimIndent()

            mockMvc.perform(
                post("/products/bulk")
                    .contentType("application/json")
                    .content(invalidPayload)
            )
                .andExpect(status().isBadRequest)
        }

    }

    @Nested
    @DisplayName("DELETE /products/{id}")
    inner class DeleteProductTest {

        @Test
        fun should_delete_product_and_return_204() {
            // given
            val id = UUID.randomUUID().toString()

            doNothing().whenever(productService).deleteProduct(id)

            // when & then
            mockMvc.perform(delete("/products/$id"))
                .andExpect(status().isNoContent)
        }

        @Test
        fun should_return_500_when_deletion_fails() {
            // given
            val id = UUID.randomUUID().toString()

            whenever(productService.deleteProduct(id))
                .thenThrow(RuntimeException("삭제 실패"))

            // when & then
            mockMvc.perform(delete("/products/$id"))
                .andExpect(status().isInternalServerError)
        }
    }

}
