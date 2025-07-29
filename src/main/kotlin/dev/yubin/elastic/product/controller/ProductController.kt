package dev.yubin.elastic.product.controller

import dev.yubin.elastic.product.service.ProductService
import dev.yubin.elastic.product.domain.Product
import dev.yubin.elastic.product.dto.CreateProductRequestDto
import dev.yubin.elastic.product.dto.ProductSearchResultDto
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("products")
class ProductController(
    private val productService: ProductService
) {

    @GetMapping
    fun getProducts(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<MutableList<Product?>> {
        val products = productService.getProducts(page, size)
        return ResponseEntity.ok(products)
    }

    @GetMapping("/search")
    fun searchProducts(
        @RequestParam query: String,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) minPrice: Double?,
        @RequestParam(required = false) maxPrice: Double?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?
    ): ResponseEntity<List<ProductSearchResultDto>> {

        val products = productService.searchProducts(query, category, minPrice, maxPrice, page, size)
        return ResponseEntity.ok(products)
    }

    @GetMapping("/suggestions")
    fun getSuggestions(
        @RequestParam() query: String,
    ): ResponseEntity<List<String>> {

        val suggestions = productService.getSuggestions(query)
        return ResponseEntity.ok(suggestions)
    }

    @PostMapping
    fun createProduct(@RequestBody @Valid dto: CreateProductRequestDto): ResponseEntity<Product> {
        val product = productService.createProduct(dto)
        return ResponseEntity.ok(product)
    }

    @PostMapping("/bulk")
    fun createProducts(@RequestBody @Valid products: List<CreateProductRequestDto>): ResponseEntity<List<Product>> {
        return ResponseEntity.ok(productService.createProducts(products))
    }

    @DeleteMapping("/{id}")
    fun deleteProduct(@PathVariable id: String): ResponseEntity<Void> {
        productService.deleteProduct(id)
        return ResponseEntity.noContent().build()
    }
}
