package com.github.jonasjesusamerico.we_sync_app.core.ui.drawer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.jonasjesusamerico.we_sync_app.R
import com.github.jonasjesusamerico.we_sync_app.databinding.ItemDrawerChildBinding
import com.github.jonasjesusamerico.we_sync_app.databinding.ItemDrawerSectionBinding

/** Rows of [DrawerMenu]: expandable sections and their sub-items. */
class DrawerAdapter(
    private val onSectionClick: (DrawerSection) -> Unit,
    private val onChildClick: (DrawerChild) -> Unit,
) : ListAdapter<DrawerRow, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is DrawerRow.Section -> TYPE_SECTION
        is DrawerRow.Child -> TYPE_CHILD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SECTION) {
            SectionHolder(ItemDrawerSectionBinding.inflate(inflater, parent, false))
        } else {
            ChildHolder(ItemDrawerChildBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is DrawerRow.Section -> (holder as SectionHolder).bind(row)
            is DrawerRow.Child -> (holder as ChildHolder).bind(row)
        }
    }

    private inner class SectionHolder(private val binding: ItemDrawerSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: DrawerRow.Section) {
            val context = binding.root.context
            binding.imageIcon.setImageResource(row.section.icon)
            binding.textTitle.setText(row.section.title)
            binding.root.isActivated = row.isSelected
            binding.imageChevron.isVisible = row.section.isExpandable
            binding.imageChevron.rotation = if (row.isExpanded) 180f else 0f
            ViewCompat.setStateDescription(
                binding.root,
                when {
                    !row.section.isExpandable -> null
                    row.isExpanded -> context.getString(R.string.state_expanded)
                    else -> context.getString(R.string.state_collapsed)
                },
            )
            binding.root.setOnClickListener { onSectionClick(row.section) }
        }
    }

    private inner class ChildHolder(private val binding: ItemDrawerChildBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: DrawerRow.Child) {
            binding.textTitle.setText(row.child.title)
            binding.root.isActivated = row.isSelected
            binding.root.setOnClickListener { onChildClick(row.child) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<DrawerRow>() {
        override fun areItemsTheSame(oldItem: DrawerRow, newItem: DrawerRow) = when {
            oldItem is DrawerRow.Section && newItem is DrawerRow.Section -> oldItem.section.key == newItem.section.key
            oldItem is DrawerRow.Child && newItem is DrawerRow.Child -> oldItem.child == newItem.child
            else -> false
        }

        override fun areContentsTheSame(oldItem: DrawerRow, newItem: DrawerRow) = oldItem == newItem
    }

    private companion object {
        const val TYPE_SECTION = 0
        const val TYPE_CHILD = 1
    }
}
