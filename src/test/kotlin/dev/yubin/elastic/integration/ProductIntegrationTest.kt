package dev.yubin.elastic.integration

import co.elastic.clients.elasticsearch._types.query_dsl.Query
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.yubin.elastic.product.domain.ProductDocument
import dev.yubin.elastic.product.dto.CreateProductRequestDto
import dev.yubin.elastic.product.repository.ProductRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ProductIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var elasticsearchOperations: ElasticsearchOperations

    val objectMapper = jacksonObjectMapper()

    companion object {
        class KMySQLContainer : MySQLContainer<KMySQLContainer>("mysql:8.0")
        class KElasticsearchContainer :
            ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.17.4")

        private val mysql = KMySQLContainer().apply {
            withDatabaseName("test-db")
            withUsername("test")
            withPassword("test")
            start()
            System.setProperty("spring.datasource.url", jdbcUrl)
            System.setProperty("spring.datasource.username", username)
            System.setProperty("spring.datasource.password", password)
        }


        val elastic = KElasticsearchContainer().apply {
            withEnv("discovery.type", "single-node")
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun overrideProps(registry: DynamicPropertyRegistry) {
            registry.add("spring.elasticsearch.uris") { elastic.httpHostAddress }
        }

        init {
            // 사용하지 않으면 경고 발생하므로 명시적으로 참조
            println("MySQL started at ${mysql.jdbcUrl}")
            println("Elasticsearch started at ${elastic.httpHostAddress}")
        }
    }

    @Test
    fun `should persist product to DB and Elasticsearch`() {
        val dto = CreateProductRequestDto("김스낵", "짭짤하고 바삭함", 1200, 4.3, "과자")
        val json = objectMapper.writeValueAsString(dto)

        val mvcResult = mockMvc.perform(
            post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isOk)
            .andReturn()

        val id = objectMapper.readTree(mvcResult.response.contentAsString)["id"].asText()

        // DB 확인
        val entity = productRepository.findById(id)
        assertThat(entity).isPresent
        assertThat(entity.get().name).isEqualTo("김스낵")

        // Elasticsearch 확인
        val query = NativeQuery.builder()
            .withQuery(Query.of { q ->
                q.match { m ->
                    m.field("name")
                        .query("김스낵")
                }
            })
            .build()

        val results = elasticsearchOperations.search(query, ProductDocument::class.java)
        assertThat(results).isNotEmpty()
    }

    @Test
    fun `should return products from Elasticsearch`() {
        val doc = ProductDocument("123", "김말이", "추억의 간식", 1500, 4.0, "간식")
        elasticsearchOperations.save(doc)

        mockMvc.perform(get("/products/search").param("query", "김"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.size()").value(1))
            .andExpect(jsonPath("$[0].name").value("김말이"))
    }

    @Test
    fun `should delete product from DB and Elasticsearch`() {
        val dto = CreateProductRequestDto("김유산균", "몸에 좋아요", 1800, 4.1, "음료")
        val json = objectMapper.writeValueAsString(dto)

        val mvcResult = mockMvc.perform(
            post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isOk)
            .andReturn()

        val id = objectMapper.readTree(mvcResult.response.contentAsString)["id"].asText()

        mockMvc.perform(delete("/products/$id"))
            .andExpect(status().isNoContent)

        assertThat(productRepository.findById(id)).isEmpty

        val query = NativeQuery.builder()
            .withQuery(Query.of { q ->
                q.match { m ->
                    m.field("id")
                        .query(id)
                }
            })
            .build()

        val results = elasticsearchOperations.search(query, ProductDocument::class.java)
        assertThat(results).isEmpty()
    }
}
