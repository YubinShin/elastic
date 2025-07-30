package dev.yubin.elastic.product.controller

import dev.yubin.elastic.product.service.ProductService
import dev.yubin.elastic.product.domain.Product
import dev.yubin.elastic.product.dto.CreateProductRequestDto
import dev.yubin.elastic.product.dto.ProductSearchResultDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.media.Content
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.media.Schema

@RestController
@RequestMapping("products")
@Validated
class ProductController(
    private val productService: ProductService
) {

    @Operation(summary = "상품 목록 조회", description = "페이지네이션으로 상품 목록을 조회합니다.")
    @GetMapping
    fun getProducts(
        @Parameter(description = "페이지 번호", example = "1")
        @RequestParam(defaultValue = "1") page: Int,
        @Parameter(description = "페이지당 아이템 수", example = "10")
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<MutableList<Product?>> {
        val products = productService.getProducts(page, size)
        return ResponseEntity.ok(products)
    }

    @Operation(summary = "상품 검색", description = "Elasticsearch 기반으로 상품을 검색합니다.")
    @GetMapping("/search")
    fun searchProducts(
        @Parameter(description = "검색어") @RequestParam query: String,
        @Parameter(description = "카테고리") @RequestParam(required = false) category: String?,
        @Parameter(description = "최소 가격") @RequestParam(required = false) minPrice: Double?,
        @Parameter(description = "최대 가격") @RequestParam(required = false) maxPrice: Double?,
        @Parameter(description = "페이지 번호") @RequestParam(required = false) page: Int?,
        @Parameter(description = "페이지 크기") @RequestParam(required = false) size: Int?
    ): ResponseEntity<List<ProductSearchResultDto>> {
        val products = productService.searchProducts(query, category, minPrice, maxPrice, page, size)
        return ResponseEntity.ok(products)
    }

    @Operation(summary = "상품 이름 자동완성", description = "입력한 검색어로 자동완성 제안을 반환합니다.")
    @GetMapping("/suggestions")
    fun getSuggestions(
        @Parameter(description = "검색어") @RequestParam query: String,
    ): ResponseEntity<List<String>> {
        val suggestions = productService.getSuggestions(query)
        return ResponseEntity.ok(suggestions)
    }

    @Operation(summary = "상품 생성", description = "상품을 하나 생성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "상품 생성 성공",
                content = [Content(schema = Schema(implementation = Product::class))]
            ),
            ApiResponse(responseCode = "400", description = "유효성 검증 실패")
        ]
    )
    @PostMapping
    fun createProduct(@RequestBody @Valid dto: CreateProductRequestDto): ResponseEntity<Product> {
        val product = productService.createProduct(dto)
        return ResponseEntity.ok(product)
    }


    @Operation(summary = "상품 여러 개 생성", description = "여러 개의 상품을 한번에 생성합니다.")
    @PostMapping("/bulk")
    fun createProducts(@RequestBody products: List<@Valid CreateProductRequestDto>): ResponseEntity<List<Product>> {
        return ResponseEntity.ok(productService.createProducts(products))
    }

    @Operation(summary = "상품 삭제", description = "상품 ID로 상품을 삭제합니다.")
    @DeleteMapping("/{id}")
    fun deleteProduct(@PathVariable id: String): ResponseEntity<Void> {
        productService.deleteProduct(id)
        return ResponseEntity.noContent().build()
    }
}
