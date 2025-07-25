package dev.yubin.elastic.product.controller

import dev.yubin.elastic.product.service.ProductService
import dev.yubin.elastic.product.domain.Product
import dev.yubin.elastic.product.domain.ProductDocument
import dev.yubin.elastic.product.dto.CreateProductRequestDto
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
        @RequestParam category: String,
        @RequestParam(defaultValue = "0.0") minPrice: Double,
        @RequestParam(defaultValue = "1000000000.0") maxPrice: Double,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "5") size: Int
    ): ResponseEntity<List<ProductDocument>> {

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
    fun createProduct(@RequestBody dto: CreateProductRequestDto): ResponseEntity<Product> {
        val product = productService.createProduct(dto)
        return ResponseEntity.ok(product)
    }

    @PostMapping("/bulk")
    fun createProducts(@RequestBody products: List<CreateProductRequestDto>): List<Product> {
        return productService.createProducts(products)
    }

    @DeleteMapping("/{id}")
    fun deleteProduct(@PathVariable id: Long): ResponseEntity<Void> {
        productService.deleteProduct(id)
        return ResponseEntity.noContent().build()
    }
}
