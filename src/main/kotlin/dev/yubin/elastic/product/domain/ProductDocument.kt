package dev.yubin.elastic.product.domain

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.*

@Document(indexName = "products")
@Setting(settingPath = "/elasticsearch/product-settings.json")
class ProductDocument(
    @Id
    var id: String,
    @MultiField(
        mainField = Field(type = FieldType.Text, analyzer = "products_name_analyzer"),
        otherFields = [InnerField(suffix = "auto_complete", type = FieldType.Search_As_You_Type, analyzer = "nori")]
    )
    var name: String,
    @Field(type = FieldType.Text, analyzer = "products_description_analyzer")
    var description: String,
    @Field(type = FieldType.Integer)
    var price: Int,
    @Field(type = FieldType.Double)
    var rating: Double,
    @MultiField(
        mainField = Field(type = FieldType.Text, analyzer = "products_category_analyzer"),
        otherFields = [InnerField(suffix = "raw", type = FieldType.Keyword)]
    )
    var category: String
) {
    companion object {
        fun from(product: Product): ProductDocument {
            return ProductDocument(
                id = product.id.toString(),
                name = product.name,
                description = product.description,
                price = product.price,
                rating = product.rating,
                category = product.category
            )
        }
    }
}
