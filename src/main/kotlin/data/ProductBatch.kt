package org.application.data // Correct package declaration

data class ProductBatch(
    val batchId: String,
    val products: List<Product>,
    val timestamp: Long
)