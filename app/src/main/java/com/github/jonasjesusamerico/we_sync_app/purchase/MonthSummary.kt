package com.github.jonasjesusamerico.we_sync_app.purchase

import java.math.BigDecimal
import java.math.RoundingMode

/** Total of one category; [name] is null for the "Outras" row that sums the categories beyond the top ones. */
data class CategoryTotal(
    val name: String?,
    val amount: BigDecimal,
    /** Bar length in 0..1, relative to the largest row. */
    val fraction: Float,
)

/** Dashboard numbers of a month. `CANCELED` entries are ignored everywhere. */
data class MonthSummary(
    val total: BigDecimal,
    val paid: BigDecimal,
    val pending: BigDecimal,
    val categories: List<CategoryTotal>,
    val latest: List<PurchaseDto>,
) {
    val isEmpty: Boolean get() = latest.isEmpty()

    companion object {
        const val TOP_CATEGORIES = 5
        const val LATEST_COUNT = 5

        private val ZERO: BigDecimal = BigDecimal.ZERO.setScale(2)

        /** [items] newest first, as returned by the API. */
        fun from(items: List<PurchaseDto>): MonthSummary {
            val active = items.filter { it.status != PurchaseStatus.CANCELED }
            val paid = active.filter { it.status == PurchaseStatus.PAID }.sum()
            val pending = active.filter { it.status == PurchaseStatus.PENDING }.sum()

            val byCategory = active
                .groupBy { it.category.id }
                .map { (_, entries) -> entries.first().category.name to entries.sum() }
                .sortedWith(compareByDescending<Pair<String, BigDecimal>> { it.second }.thenBy { it.first })
            val rows = byCategory.take(TOP_CATEGORIES).toMutableList<Pair<String?, BigDecimal>>()
            val others = byCategory.drop(TOP_CATEGORIES)
            if (others.isNotEmpty()) rows += null to others.fold(ZERO) { acc, it -> acc + it.second }

            val largest = rows.maxOfOrNull { it.second } ?: ZERO
            return MonthSummary(
                total = paid + pending,
                paid = paid,
                pending = pending,
                categories = rows.map { (name, amount) -> CategoryTotal(name, amount, fractionOf(amount, largest)) },
                latest = active.take(LATEST_COUNT),
            )
        }

        private fun List<PurchaseDto>.sum(): BigDecimal = fold(ZERO) { acc, it -> acc + it.amount }

        private fun fractionOf(amount: BigDecimal, largest: BigDecimal): Float =
            if (largest.signum() == 0) 0f else amount.divide(largest, 4, RoundingMode.HALF_UP).toFloat()
    }
}
