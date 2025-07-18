package org.application.data

import java.util.*

/**
 * Represents a batch of products, typically used for bulk operations
 * like sending multiple products to an API endpoint.
 * This is a data class, primarily used to hold data, and automatically
 * provides useful functions like `equals()`, `hashCode()`, `toString()`,
 * and `copy()`.
 *
 * @property batchId A unique identifier for this specific product batch.
 * @property products A [List] of [Product] objects included in this batch.
 * @property timestamp A [Long] value representing the creation or processing
 * timestamp of the batch, typically in milliseconds since the Unix epoch.
 */
data class ProductBatch(
    val batchId: UUID,
    val products: List<Product>,
    val timestamp: Long
)
