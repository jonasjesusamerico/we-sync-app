package com.github.jonasjesusamerico.we_sync_app.purchase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MonthSummaryTest {

    @Test
    fun totalsAreExactAndIgnoreCanceled() {
        val summary = MonthSummary.from(
            listOf(
                purchase(1, "0.10"),
                purchase(2, "0.20", status = PurchaseStatus.PENDING),
                purchase(3, "1000.00", status = PurchaseStatus.CANCELED),
                purchase(4, "99999999.99"),
            ),
        )

        assertEquals(BigDecimal("100000000.09"), summary.paid)
        assertEquals(BigDecimal("0.20"), summary.pending)
        assertEquals(BigDecimal("100000000.29"), summary.total)
        assertEquals(listOf(1L, 2L, 4L), summary.latest.map { it.id })
    }

    @Test
    fun topFiveCategoriesPlusOthers_barsRelativeToLargest() {
        val items = (1L..7L).map { id -> purchase(id, "${id * 10}.00", categoryId = id) }

        val categories = MonthSummary.from(items).categories

        assertEquals(
            listOf("Categoria 7", "Categoria 6", "Categoria 5", "Categoria 4", "Categoria 3", null),
            categories.map { it.name },
        )
        assertEquals(BigDecimal("30.00"), categories.last().amount) // 10 + 20
        assertEquals(1f, categories.first().fraction)
        assertEquals(30f / 70f, categories.last().fraction, 0.001f)
    }

    @Test
    fun sameCategoryIsSummed_tiesOrderedByName() {
        val categories = MonthSummary.from(
            listOf(
                purchase(1, "5.00", categoryId = 2, categoryName = "Bebidas"),
                purchase(2, "5.00", categoryId = 1, categoryName = "Alimentação"),
                purchase(3, "2.50", categoryId = 2, categoryName = "Bebidas"),
                purchase(4, "2.50", categoryId = 1, categoryName = "Alimentação"),
            ),
        ).categories

        assertEquals(listOf("Alimentação", "Bebidas"), categories.map { it.name })
        assertEquals(listOf(BigDecimal("7.50"), BigDecimal("7.50")), categories.map { it.amount })
    }

    @Test
    fun latestIsLimitedToFiveInApiOrder() {
        val latest = MonthSummary.from((1L..8L).map { purchase(it, "1.00") }).latest

        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), latest.map { it.id })
    }

    @Test
    fun emptyMonth() {
        val summary = MonthSummary.from(listOf(purchase(1, "5.00", status = PurchaseStatus.CANCELED)))

        assertTrue(summary.isEmpty)
        assertEquals(BigDecimal("0.00"), summary.total)
        assertTrue(summary.categories.isEmpty())
    }
}
