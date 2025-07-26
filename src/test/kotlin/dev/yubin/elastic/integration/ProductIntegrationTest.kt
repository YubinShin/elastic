package dev.yubin.elastic.integration

import com.fasterxml.jackson.databind.ObjectMapper
import dev.yubin.elastic.product.dto.CreateProductRequestDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.MySQLContainer

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    // Elasticsearch는 실제 연결 안함. 모킹만.
    @MockBean
    lateinit var elasticsearch: org.springframework.data.elasticsearch.core.ElasticsearchOperations

    companion object {
        // MySQL Testcontainer (Optional)
        class KMySQLContainer : MySQLContainer<KMySQLContainer>("mysql:8.0")
        val mysql = KMySQLContainer().apply {
            withDatabaseName("test-db")
            withUsername("test")
            withPassword("test")
            start()
            System.setProperty("spring.datasource.url", jdbcUrl)
            System.setProperty("spring.datasource.username", username)
            System.setProperty("spring.datasource.password", password)
        }
    }

    @Test
    fun `상품 등록 후 목록 조회까지 성공`() {
        val dto = CreateProductRequestDto("김라면", "매운맛", 1200, 4.5, "식품")

        // 1. 상품 등록
        mockMvc.perform(
            post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("김라면"))

        // 2. 목록 조회 (등록한 게 나오는지 확인)
        mockMvc.perform(get("/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size()").value(1))
            .andExpect(jsonPath("$[0].name").value("김라면"))
    }
}
