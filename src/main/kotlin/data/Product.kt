package org.application.data

import java.util.UUID

/**
 * Represents a single product with its key attributes.
 * This is a data class, primarily used to hold data, and automatically
 * provides useful functions like `equals()`, `hashCode()`, `toString()`,
 * and `copy()`.
 *
 * @property id A unique identifier for the product as a UUID.
 * @property name The name of the product (e.g., "Laptop").
 * @property price The price of the product.
 * @property category The category to which the product belongs (e.g., "Electronics").
 * @property inStock A boolean indicating whether the product is currently in stock.
 */
data class Product(
    val id: UUID,
    val name: String,
    val price: Double,
    val category: String,
    val inStock: Boolean
)