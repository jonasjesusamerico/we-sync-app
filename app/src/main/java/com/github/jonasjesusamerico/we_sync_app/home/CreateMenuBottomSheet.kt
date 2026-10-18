package com.github.jonasjesusamerico.we_sync_app.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.github.jonasjesusamerico.we_sync_app.R
import com.github.jonasjesusamerico.we_sync_app.core.ui.navigateFrom
import com.github.jonasjesusamerico.we_sync_app.databinding.BottomSheetCreateMenuBinding
import com.github.jonasjesusamerico.we_sync_app.databinding.ItemCreateOptionBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** "+" menu: what the user can create. New options are one entry in [options]. */
class CreateMenuBottomSheet : BottomSheetDialogFragment() {

    private data class CreateOption(
        @DrawableRes val icon: Int,
        @StringRes val title: Int,
        @StringRes val subtitle: Int,
        @IdRes val action: Int,
    )

    private val options = listOf(
        CreateOption(
            icon = R.drawable.ic_receipt,
            title = R.string.create_option_purchase_title,
            subtitle = R.string.create_option_purchase_subtitle,
            action = R.id.action_createMenu_to_purchaseForm,
        ),
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = BottomSheetCreateMenuBinding.inflate(inflater, container, false)
        options.forEach { option ->
            val row = ItemCreateOptionBinding.inflate(inflater, binding.listOptions, true)
            row.imageIcon.setImageResource(option.icon)
            row.textTitle.setText(option.title)
            row.textSubtitle.setText(option.subtitle)
            row.root.setOnClickListener { navigateFrom(R.id.createMenuSheet, option.action) }
        }
        return binding.root
    }
}
