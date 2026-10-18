package com.github.jonasjesusamerico.we_sync_app.purchase

import com.github.jonasjesusamerico.we_sync_app.category.CategoryDto
import com.github.jonasjesusamerico.we_sync_app.paymentmethod.PaymentMethodDto
import java.math.BigDecimal
import java.time.LocalDate

internal fun purchase(
    id: Long,
    amount: String,
    categoryId: Long = 1,
    categoryName: String = "Categoria $categoryId",
    status: PurchaseStatus = PurchaseStatus.PAID,
    date: LocalDate = LocalDate.of(2026, 10, 1),
) = PurchaseDto(
    id = id,
    date = date,
    description = "Entry $id",
    category = CategoryDto(categoryId, categoryName, true),
    paymentMethod = PaymentMethodDto(3, "Pix", true),
    amount = BigDecimal(amount),
    status = status,
)

internal fun purchaseJson(id: Long, amount: String = "10.00", status: String = "PAID") = """
    {"id":$id,"user_id":7,"purchase_date":"2026-10-17","description":"Entry $id",
     "category":{"id":1,"name":"Alimentação","is_global":true},
     "payment_method":{"id":3,"name":"Pix","is_global":true},
     "amount":"$amount","status":"$status","created_at":"x","updated_at":"x"}
""".trimIndent()
