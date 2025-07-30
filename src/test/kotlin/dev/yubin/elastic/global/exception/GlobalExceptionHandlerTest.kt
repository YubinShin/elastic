package dev.yubin.elastic.global.exception

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.yubin.elastic.product.dto.CreateProductRequestDto
import dev.yubin.elastic.product.service.ProductService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.containsString
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath


@WebMvcTest
@Import(GlobalExceptionHandler::class, GlobalExceptionHandlerTest.MockConfig::class)
class GlobalExceptionHandlerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var productService: ProductService

    @TestConfiguration
    class MockConfig {
        @Bean
        fun productService(): ProductService = mock()
    }

    private val objectMapper = ObjectMapper().registerKotlinModule()

    fun Any.toJson(): String = objectMapper.writeValueAsString(this)

    fun dummyCreateRequest() = CreateProductRequestDto(
        name = "", // NotBlank 어기는 값
        description = "맛있음",
        price = 3000,
        rating = 4.5,
        category = "식품"
    )

    @Test
    fun should_return_400_when_field_validation_fails() {
        val invalidRequest = dummyCreateRequest()

        mockMvc.post("/products") {
            contentType = MediaType.APPLICATION_JSON
            content = invalidRequest.toJson()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_REQUEST") }
        }
    }

    @Test
    fun should_return_400_when_json_is_malformed() {
        val malformedJson = """{ "name": "김밥", "price": }""" // price 비어있음

        mockMvc.post("/products") {
            contentType = MediaType.APPLICATION_JSON
            content = malformedJson
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_JSON") }
        }
    }

    @Test
    fun should_return_500_when_custom_exception_is_thrown() {
        whenever(productService.createProduct(any()))
            .thenThrow(CustomException("SOMETHING_WRONG", "unexpected"))

        val request = CreateProductRequestDto("돌김", "도시락", 3000, 4.5, "식품")

        mockMvc.post("/products") {
            contentType = MediaType.APPLICATION_JSON
            content = request.toJson()
        }.andExpect {
            status { isInternalServerError() }
            jsonPath("$.code") { value("SOMETHING_WRONG") }
            jsonPath("$.message") { value("unexpected") }
        }
    }

    @Test
    fun should_return_500_when_runtime_exception_occurs() {
        whenever(productService.getProducts(any(), any()))
            .thenThrow(RuntimeException("unexpected"))

        mockMvc.get("/products")
            .andExpect {
                status { isInternalServerError() }
                jsonPath("$.code") { value("INTERNAL_ERROR") }
                jsonPath("$.message") { value("unexpected") }
            }
    }

    @Test
    fun should_return_custom_error_when_custom_exception_thrown() {
        whenever(productService.getProducts(1, 5))
            .thenThrow(CustomException("MY_ERROR", "unexpected"))

        mockMvc.perform(get("/products")
            .param("page", "1")
            .param("size", "5"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.code").value("MY_ERROR"))
            .andExpect(jsonPath("$.message").value("unexpected"))
    }

    @Test
    fun should_return_invalid_json_when_body_is_unreadable() {
        mockMvc.perform(
            post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"name\": \"test\" ") // 닫는 중괄호 없는 malformed JSON
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_JSON"))
            .andExpect(jsonPath("$.message").value("요청 바디가 잘못되었습니다."))
    }

    @Test
    fun should_return_invalid_request_when_constraint_violated() {
        mockMvc.perform(
            get("/products")
                .param("page", "-1")
                .param("size", "10")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value(containsString("must be greater")))
    }

}
