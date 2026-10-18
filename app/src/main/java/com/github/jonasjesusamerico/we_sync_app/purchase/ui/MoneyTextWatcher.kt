package com.github.jonasjesusamerico.we_sync_app.purchase.ui

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.github.jonasjesusamerico.we_sync_app.purchase.MoneyInput
import java.math.BigDecimal

/** Keeps [field] formatted as money (see [MoneyInput]) and reports each new value. */
class MoneyTextWatcher(
    private val field: EditText,
    private val onAmountChanged: (BigDecimal) -> Unit,
) : TextWatcher {
    private var updating = false

    /** Shows [amount] without reporting it back. */
    fun show(amount: BigDecimal) {
        val formatted = MoneyInput.format(amount)
        if (field.text.toString() != formatted) replaceText(formatted)
    }

    override fun afterTextChanged(s: Editable) {
        if (updating) return
        val amount = MoneyInput.parse(s.toString())
        val formatted = MoneyInput.format(amount)
        if (s.toString() != formatted) replaceText(formatted)
        onAmountChanged(amount)
    }

    private fun replaceText(text: String) {
        updating = true
        field.setText(text)
        field.setSelection(text.length)
        updating = false
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
}
