package com.github.jonasjesusamerico.we_sync_app.core.ui.drawer

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.github.jonasjesusamerico.we_sync_app.R

/** What a drawer item does; handled by `MainActivity`. */
enum class DrawerAction { HOME, NEW_PURCHASE, LOGOUT }

data class DrawerChild(@StringRes val title: Int, val action: DrawerAction)

/** A top-level item: navigates directly ([action]) or expands into [children]. */
data class DrawerSection(
    val key: String,
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    val action: DrawerAction? = null,
    val children: List<DrawerChild> = emptyList(),
) {
    val isExpandable: Boolean get() = children.isNotEmpty()
}

sealed interface DrawerRow {
    data class Section(val section: DrawerSection, val isExpanded: Boolean, val isSelected: Boolean) : DrawerRow
    data class Child(val child: DrawerChild, val isSelected: Boolean) : DrawerRow
}

/**
 * Drawer content. Only screens that exist are listed (spec 003, D-2): a new screen adds its item here.
 */
object DrawerMenu {
    val sections = listOf(
        DrawerSection(key = "home", title = R.string.drawer_home, icon = R.drawable.ic_home, action = DrawerAction.HOME),
        DrawerSection(
            key = "purchases",
            title = R.string.drawer_purchases,
            icon = R.drawable.ic_receipt,
            children = listOf(DrawerChild(R.string.drawer_new_purchase, DrawerAction.NEW_PURCHASE)),
        ),
        DrawerSection(
            key = "account",
            title = R.string.drawer_account,
            icon = R.drawable.ic_account,
            children = listOf(DrawerChild(R.string.action_logout, DrawerAction.LOGOUT)),
        ),
    )

    /** Visible rows: every section, plus the children of the [expanded] ones. */
    fun rows(expanded: Set<String>, selected: DrawerAction?): List<DrawerRow> = sections.flatMap { section ->
        val isExpanded = section.isExpandable && section.key in expanded
        val sectionRow = DrawerRow.Section(section, isExpanded, isSelected = section.action != null && section.action == selected)
        if (!isExpanded) {
            listOf(sectionRow)
        } else {
            listOf(sectionRow) + section.children.map { DrawerRow.Child(it, isSelected = it.action == selected) }
        }
    }

    fun toggle(expanded: Set<String>, key: String): Set<String> = if (key in expanded) expanded - key else expanded + key
}
